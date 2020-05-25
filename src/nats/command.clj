(ns nats.command
  (:import [io.netty.buffer Unpooled]
           [io.netty.util CharsetUtil]))


(defn- sid [] (str (java.util.UUID/randomUUID)))

(def ping (Unpooled/copiedBuffer "PING\r\n" CharsetUtil/UTF_8))

(def pong (Unpooled/copiedBuffer "PONG\r\n" CharsetUtil/UTF_8))

(def connect (Unpooled/copiedBuffer
              "CONNECT {\"verbose\":false,\"lang\":\"clojure\"}\r\n"
              CharsetUtil/UTF_8))

(defn sub-string
  ([subject] (sub-string subject nil))
  ([subject group] (str "SUB " subject
                        (when (some? group)
                          (str " " group))
                        " " (sid) "\r\n")))

(defn pub [subject payload]
  (let [buf (Unpooled/buffer)]
    (.writeBytes buf (.getBytes (str "PUB " subject " " (count payload) "\r\n") "UTF-8"))
    (.writeBytes buf payload)
    (.writeBytes buf (byte-array [(byte \return)
                                  (byte \newline)]))))
