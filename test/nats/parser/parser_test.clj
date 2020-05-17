(ns nats.parser.parser-test
  (:require [clojure.test :refer [deftest testing is]])
  (:import [io.netty.buffer ByteBuf Unpooled ByteBufProcessor]
           [nats.parser Parser Info Ping Pong]))

(defn str->bytes [s]
  (byte-array (count s) (map byte s)))

(defn bytes->str [buf]
  (apply str (map char buf)))

(defn str->ByteBuf [s] (-> s
                           str->bytes
                           (Unpooled/copiedBuffer)))

(defn ByteBuf->str [buf] (-> buf
                             (.slice)
                             (.array)
                             bytes->str))

(deftest parser
  (is (= Info
         (-> "INFO {\"server_id\":\"A0FE123\"} \r\n"
             str->ByteBuf
             (Parser/parseMessage)
             type)))
  (is (= "INFO {\"server_id\":\"A0FE123\"} "
         (-> "INFO {\"server_id\":\"A0FE123\"} \r\n"
             str->ByteBuf
             (Parser/parseMessage)
             (.getMessage))))
  (is (= Ping
         (-> "PING\r\n"
             str->ByteBuf
             (Parser/parseMessage)
             type)))
  (is (= Pong
         (-> "PONG\r\n"
             str->ByteBuf
             (Parser/parseMessage)
             type))))


(byte \P)
