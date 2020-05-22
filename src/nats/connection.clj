(ns nats.connection
  (:require [nats.decode :as decode])
  (:import [io.netty.bootstrap Bootstrap]
           [io.netty.buffer ByteBuf Unpooled]
           [io.netty.channel ChannelInboundHandler ChannelInitializer]
           [io.netty.channel.nio NioEventLoopGroup]
           [io.netty.channel.socket.nio NioSocketChannel]
           [io.netty.util CharsetUtil]
           [java.net InetSocketAddress]))

(def default-options
  {:host "localhost"
   :port 4222})

(defn channel-handler []
  (reify ChannelInboundHandler
    ; the channel of the ctx has been registered with its eventloop
    (channelRegistered [this ctx]
      (println "registered")
      (.fireChannelRegistered ctx))
    ; the channel of the ctx has been unregistered from its eventloop
    (channelUnregistered [this ctx]
      (println "unregistered")
      (.fireChannelUnregistered ctx))
    ; the channel of the ctx is now active
    (channelActive [this ctx]
      (println "active")
      (.writeAndFlush
       ctx
       (Unpooled/copiedBuffer "PING\r\n" CharsetUtil/UTF_8))
      (.fireChannelActive ctx))
    ; the channel of the ctx is now inactive
    (channelInactive [this ctx]
      (println "inactive")
      (.fireChannelInactive ctx))
    ; invoked when the channel has read a message
    (channelRead [this ctx msg]
      (println "read")
      ;(.fireChannelRead ctx)
      ; the buffer represents the data from a single read op
      ; need to buffer the buffer
      (let [buf     (cast ByteBuf msg)
            content (.toString
                     buf
                     CharsetUtil/UTF_8)]
        (println "readable bytes:" (.readableBytes buf))
        (when-let [msg (decode/parse msg)]
          (println "msg:" msg))
        (println "readable bytes:" (.readableBytes buf))
        (println "actual content <" content ">")))
    ; invoked when the last message from a CURRENT read operation
    ; has been consumed from channelRead
    (channelReadComplete [this ctx]
      (println "readComplete")
      ;(.fireChannelReadComplete ctx)
      )
    ; an user event was triggered
    (userEventTriggered [this ctx event]
      (println "event triggered")
      (.fireUserEventTriggered ctx event))
    ; called once the writable state of a channel changed
    (channelWritabilityChanged [this ctx]
      (println "writable changed")
      (.fireChannelWritabilityChanged ctx))
    ; called if throwable was thrown
    (exceptionCaught [this ctx cause]
      (println "exception")
      (.printStackTrace cause)
      (.close ctx))
    
    ; inherited from ChannelHandler

    ; called after the handler was added to the ctx
    (handlerAdded [this ctx]
      (println "added"))
    ; called after the handler was removed from the ctx
    (handlerRemoved [this ctx]
      (println "removed"))))

(defn initializer []
  (proxy [ChannelInitializer] []
    (initChannel [ch]
      (.addLast
       (.pipeline ch)
       (channel-handler)))))

(defn connect
  ([] (connect default-options))
  ([{:keys [host port]}]
   ;; create connection to nats via tcp
   ;; implement reconnect
   (let [g (NioEventLoopGroup.)
         b (Bootstrap.)]
     (.. b
         (group g)
         (channel NioSocketChannel)
         (remoteAddress (InetSocketAddress. host port))
         (handler (initializer)))
     (let [channel-future (.sync (.connect b))]
       (reify
         java.io.Closeable
         (close [this]
           (println "close the connection")
           (try
             (.sync
              (.close
               (.channel channel-future)))
             (finally
               (println "close the eventgroup")
               (.sync
                (.shutdownGracefully g))))))))))
