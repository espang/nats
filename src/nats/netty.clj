(ns nats.netty
  (:import
   [nats ClientHandler Initializer MessageHandler]
   [io.netty.buffer ByteBuf]
   [io.netty.bootstrap Bootstrap]
   [io.netty.channel ChannelInitializer
                     ChannelOption
                     ChannelInboundHandlerAdapter
                     ChannelHandler]
   [io.netty.channel.nio NioEventLoopGroup]
   [io.netty.channel.socket.nio NioSocketChannel]
   [java.nio.charset StandardCharsets]
   [io.netty.channel.socket SocketChannel]))

(defn message-handler
  "Make a handler for a message received from nats. That
   is one-of: {INFO, PING, PONG, +OK, -ERR, MSG}."
  [f]
  (reify MessageHandler
    (onMessage [_ s] (f s))))

(defn client
  "start a tcp client that connect to a nats server 
  on host:port and calls handler for each message
  received."
  [host port handler]
  (let [workerGroup (NioEventLoopGroup.)]
    (try
      (let [b (Bootstrap.)]
        (.group b workerGroup)
        (.channel b NioSocketChannel)
        (.option b ChannelOption/SO_KEEPALIVE true)
        (.handler b (Initializer. (ClientHandler. handler)))
        (let [f (.sync (.connect b host port))]
          (.sync (.closeFuture (.channel f)))))
      (catch Exception e
        (.printStackTrace e))
      (finally
        (.shutdownGracefully workerGroup)))))

(defn stop [x])

(comment
  (client "localhost" 4222 (message-handler (fn [msg] (println msg)))))
