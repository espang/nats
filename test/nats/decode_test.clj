(ns nats.decode-test
  (:require [clojure.test :refer [deftest is]]
            [nats.decode :as SUT])
  (:import [io.netty.buffer ByteBuf Unpooled ByteBufProcessor]
           [io.netty.util CharsetUtil]))

(defn buf-from
  "Create a ByteBuf from a string for testing.
  If given a prefix it will also update the readerIndex
  to point to the first byte of the string."
  ([s]
   (Unpooled/copiedBuffer s CharsetUtil/UTF_8))
  ([prefix s]
   (let [size-in-bytes (alength (.getBytes prefix "UTF-8"))]
     (.readerIndex
      (Unpooled/copiedBuffer (str prefix s) CharsetUtil/UTF_8)
      size-in-bytes))))

(deftest uncomplete
  (is (= nil (SUT/parse (buf-from "PIN")))))

(deftest ping
  (is (= {:msg/type :ping} (SUT/parse (buf-from "PING\r\n"))))
  (is (= {:msg/type :ping} (SUT/parse (buf-from "ping\r\n")))))

(deftest pong
  (is (= {:msg/type :pong} (SUT/parse (buf-from "PONG\r\n"))))
  (is (= {:msg/type :pong} (SUT/parse (buf-from "pong\r\n")))))

(deftest info
  (is (= {:msg/type :info
          :content  {"server_id" 123}}
         (SUT/parse (buf-from "INFO  \t {\"server_id\":123} \r\n")))))

(deftest msg
  (is (= {:msg/type :msg
          :content {:subject  "foo"
                    :sid      "bar"
                    :reply-to nil
                    :payload  "Ha"}}
         (update-in (SUT/parse (buf-from "MSG foo bar 2\r\nHa\r\n"))
                    [:content :payload]
                    (fn [a]
                      (String. a))))))
