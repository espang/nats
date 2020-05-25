(ns nats.decode-test
  (:require [clojure.test :refer [deftest is testing]]
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

(defn append [buf text]
  (.writeBytes buf (.getBytes text "UTF-8")))

(deftest incomplete
  (is (= nil (SUT/parse (buf-from "PIN"))))
  (is (= nil (SUT/parse (buf-from "MSG "))))
  (testing "shouldn't change the buffer when reading an incomplete message"
    (let [buf   (buf-from "pong\r\n" "msg")
          r-idx (.readerIndex buf)
          msg   (SUT/parse buf)]
      (is (= nil msg))
      (is (= r-idx
             (.readerIndex buf))))))

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

(deftest multiple-messages
  (let [buf (buf-from "ping\r\n+ok\r\nMSG foo bar 2\r\nHa\r\n")]
    (is (= :ping (:msg/type (SUT/parse buf))))
    (is (= :ok   (:msg/type (SUT/parse buf))))
    (is (= :msg  (:msg/type (SUT/parse buf)))))
  (let [buf (buf-from "ping\r\n+ok\r\nMSG")]
    (is (= :ping (:msg/type (SUT/parse buf))))
    (is (= :ok   (:msg/type (SUT/parse buf))))
    (is (= nil   (SUT/parse buf))))
  (let [buf (buf-from "MSG foo bf393ba9-d7be-4f67-9084-160f82c67c01 15\r\nhello foo!16397\r\nMSG")]
    (is (= :msg (:msg/type (SUT/parse buf))))
    (let [buf (append buf "")]
      (is (= nil (SUT/parse buf)))
      (let [buf (append buf " foo bf393ba9-d7be-4f67-9084-160f82c67c01 15\r\nhello foo!16398\r\n")]
        (is (= :msg (:msg/type (SUT/parse buf)))))))
  (let [buf (buf-from "MSG foo bf393ba9-d7be-4f67-9084-160f82c67c01 15\r\nhello foo!16397\r\nMSG")]
    (println (.readerIndex buf))
    (is (= :msg (:msg/type (SUT/parse buf))))
    (let [buf (append buf "")]
      (println (.readerIndex buf))
      (is (= nil (SUT/parse buf)))
      (let [buf (append buf " ")]
        (println (.readerIndex buf))
        (is (= nil (SUT/parse buf)))))))
