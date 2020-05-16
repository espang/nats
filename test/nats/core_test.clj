(ns nats.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [nats.core :as nats]))

(def info-message
  "INFO {\"server_id\":\"NCTRX7SIXJMRDFAZLIMRUEIYWF5VTFJAWGHGXMELSAUUA5CZ7LXS3WO2\",\"server_name\":\"NCTRX7SIXJMRDFAZLIMRUEIYWF5VTFJAWGHGXMELSAUUA5CZ7LXS3WO2\",\"version\":\"2.1.6\",\"proto\":1,\"go\":\"go1.14.2\",\"host\":\"0.0.0.0\",\"port\":4222,\"max_payload\":1048576,\"client_id\":5,\"client_ip\":\"127.0.0.1\"} \r\n")

(def ping-message "PING\r\n")

(def pong-message "PONG\r\n")

(deftest understand-the-commands
  (testing "INFO"
    (is (= {:command "INFO"
            :payload " {\"server_id\":\"NCTRX7SIXJMRDFAZLIMRUEIYWF5VTFJAWGHGXMELSAUUA5CZ7LXS3WO2\",\"server_name\":\"NCTRX7SIXJMRDFAZLIMRUEIYWF5VTFJAWGHGXMELSAUUA5CZ7LXS3WO2\",\"version\":\"2.1.6\",\"proto\":1,\"go\":\"go1.14.2\",\"host\":\"0.0.0.0\",\"port\":4222,\"max_payload\":1048576,\"client_id\":5,\"client_ip\":\"127.0.0.1\"} \r\n"}
           (nats/incoming-message (.getBytes info-message)))))
  (testing "PING"
    (is (= {:command "PING"
            :payload "\r\n"}
           (nats/incoming-message (.getBytes ping-message)))))
  (testing "PONG"
    (is (= {:command "PONG"
            :payload "\r\n"}
           (nats/incoming-message (.getBytes pong-message))))))
