(ns nats.connection
  (:require [clojure.core.async :as async] 
            [nats.decode :as decode]
            [nats.command :as command]
            [nats.handler :as h])
  (:import [io.netty.bootstrap Bootstrap]
           [io.netty.buffer ByteBuf Unpooled]
           [io.netty.channel ChannelInboundHandler ChannelInitializer ChannelFutureListener]
           [io.netty.channel.nio NioEventLoopGroup]
           [io.netty.channel.socket.nio NioSocketChannel]
           [io.netty.util CharsetUtil]
           [java.net InetSocketAddress]
           [java.util.zip CRC32]))

(def default-options {:host "localhost"
                      :port 4222})

(defn initializer [chan]
  (proxy [ChannelInitializer] []
    (initChannel [ch]
      (.addLast (.pipeline ch) (h/->NatsChannelHandler nil chan)))))

(defn future-listener [label]
  (proxy [ChannelFutureListener] []
    (operationComplete [future]
      (when (not (.isSuccess future))
         (println "failure:" label)
         (.printStacktrace
          (.cause future))))))

(defrecord Connection
    [channel
     event-group
     
     maintain-chan ; handle internal comms
     subs-chan     ; handle subscriptions

     running ; atom holding a boolean value
     info
     state
     crc]
  java.io.Closeable
  (close [this]
    (reset! running false)
    (println "close the connection")
    (try
      (.sync (.close channel))
      (finally
        (println "close the eventgroup")
        (.sync (.shutdownGracefully event-group))))))

(defn value [conn]
  (.getValue (:crc conn)))

(defn write-to [conn buf]
  (let [fut (.writeAndFlush (:channel conn) buf)]
    (.addListener
     fut
     (future-listener "h"))))

(defn sub [conn subject]
  (let [s (command/sub-string subject)]
    (write-to conn (.retain (Unpooled/copiedBuffer s CharsetUtil/UTF_8)))))

(defn pub [conn subject payload]
  (write-to conn (.retain (command/pub subject payload))))

(defn make-connection [channel event-group pub]
  (let [c1         (async/chan)
        c2         (async/chan 100)
        running    (atom true)
        info       (atom nil)
        state      (atom {})
        crc        (CRC32.)]

    (async/sub pub :info c1)
    (async/sub pub :ping c1)
    (async/sub pub :pong c1)
    (async/sub pub :ok c1)
    (async/go-loop []
      (if @running
        (let [{:keys [msg/type] :as msg} (async/<! c1)]
          (println msg)
          (case type
            :info (reset! info (:conent msg))
            :ping (.writeAndFlush channel (.retain command/pong))
            :pong (swap! state assoc :last-pong (System/currentTimeMillis))
            :ok   nil)
          (recur))
        (println
         "Stopping connection go routine")))

    (async/sub pub :msg c2)
    (async/go-loop []
      (if @running
        (let [{:keys [content]} (async/<! c2)]
          (swap! state update :msgs (fnil inc 0))
          (.update crc (:payload content))
          (recur))
        (println
         "Stopping subscription handler")))

    (->Connection channel
                  event-group
                  c1
                  c2
                  running
                  info
                  state
                  crc)))

(defn connect
  ([] (connect default-options))
  ([{:keys [host port]}]
   (let [; small buffer for incoming messages - allows to start go-routines after creating the connection
         c (async/chan 100)
         p (async/pub c :msg/type)
         g (NioEventLoopGroup.)
         b (Bootstrap.)]
     (.. b
         (group g)
         (channel NioSocketChannel)
         (remoteAddress (InetSocketAddress. host port))
         (handler (initializer c)))
     (let [channel-future (.sync (.connect b))
           conn (make-connection (.channel channel-future)
                                 g
                                 p)]
       (println "write connect")
       (write-to conn (.retain command/connect))
       (println "write ping")
       (write-to conn (.retain command/ping))
       conn))))
