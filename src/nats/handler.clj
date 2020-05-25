(ns nats.handler
  (:require [clojure.core.async :as async]
            [nats.decode :as decode])
  (:import [io.netty.buffer ByteBuf Unpooled]
           [io.netty.channel ChannelInboundHandler]
           [io.netty.util CharsetUtil]))

(def buffer-size (* 16 1024 1024))

(def max-message-size (* 1024 1024))

(def discard-limit (+ 500 max-message-size))

(defprotocol IStatefulNatsHandler
  (append [this buf])
  (parse [this]))

(deftype NatsChannelHandler [^{:unsynchronized-mutable true} buf
                             chan]
  IStatefulNatsHandler
  (append [this msg]
    (.writeBytes buf msg))
  (parse  [this]
    (let [msg (decode/parse buf)]
      (if (= :protocol-error msg)
        (do
          (println (str "error parsing: '"
                        (.toString buf
                                   (- (.readerIndex buf) 100)
                                   100
                                   CharsetUtil/UTF_8)
                        "::"
                        (.toString buf
                                   (.readerIndex buf)
                                   20
                                   CharsetUtil/UTF_8)
                        "'"
                        (.readerIndex buf)
                        (.writerIndex buf)))
          (let [arr (byte-array 10)]
            (.getBytes buf (- (.readerIndex buf) 10) arr)
            (println "bytes:" (vec arr)))
          false)
        (if (some? msg)
          (if (async/>!! chan msg)
            true
            (do 
              (println "dropped message:'" msg "'\n")
              true))
          false))))

  ChannelInboundHandler
  (channelRegistered [this ctx] 
    (.fireChannelRegistered ctx))

  (channelUnregistered [this ctx]
    (.fireChannelUnregistered ctx))

  (channelActive [this ctx]
    (.fireChannelActive ctx))

  (channelInactive [this ctx]
    (.fireChannelInactive ctx))

  (channelRead [this ctx msg]
    (let [b (cast ByteBuf msg)]
      (.append this b)
      (.release b))
    ;; handle all messages in the buffer
    (while (.parse this))
    ;; make space when we run out of space.
    ;; make sure that we can free enought space
    ;; consuming to slow otherwise
    (when (and (< (.maxWritableBytes buf)
                  discard-limit)
               (> (/ (.readerIndex buf)
                     buffer-size)
                  0.5))
      (.discardReadBytes buf)))
  
  (channelReadComplete [this ctx]
    (.fireChannelReadComplete ctx))
  
  (userEventTriggered [this ctx event]
    (.fireUserEventTriggered ctx event))

  (channelWritabilityChanged [this ctx]
    (.fireChannelWritabilityChanged ctx))

  (exceptionCaught [this ctx cause]
    (.printStackTrace cause)
    (.close ctx))
  
  ; inherited from ChannelHandler
  (handlerAdded [this ctx]
    (set! buf (.retain (.buffer (.alloc ctx) (* 4 1024 1024)))))
  (handlerRemoved [this ctx]
    (.release buf)
    (set! buf nil)))
