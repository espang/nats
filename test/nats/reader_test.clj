(ns nats.reader-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.core.async :as async]
            [nats.reader :as reader]))

(defn ^:private str->bytes [s]
  (byte-array (map byte s)))

(deftest reader-lifecycle
  (let [messages (async/chan)
        r        (reader/make-reader messages)
        pub      (reader/get-pub r)
        ch-info  (async/chan 1)
        sub-info (async/sub pub :info ch-info)
        ch-ping  (async/chan 2)
        sub-ping (async/sub pub :ping ch-ping)
        ch-pong  (async/chan 3)
        sub-pong (async/sub pub :pong ch-pong)
        ch-unkn  (async/chan 10)
        sub-unkn (async/sub pub :error ch-unkn)]
    (async/put! messages (str->bytes "INFO {\"server_id\":123} \r\n"))
    (async/put! messages (str->bytes "PONG\r\n"))
    (async/put! messages (str->bytes "PING\r\n"))
    (async/put! messages (str->bytes "PONG\r\n"))
    (async/put! messages (str->bytes "PING\r\n"))
    (async/put! messages (str->bytes "PONG\r\n"))
    (async/put! messages (str->bytes "STOP\r\n"))
    ;; remove that:
    (async/<!! (async/timeout 100))
        
    (is (= {:command :info
            :content {"server_id" 123}}
           (async/poll! ch-info)))
    
    (is (= {:command :ping}
           (async/poll! ch-ping)))
    (is (= {:command :ping}
           (async/poll! ch-ping)))

    (is (= {:command :pong}
           (async/poll! ch-pong)))
    (is (= {:command :pong}
           (async/poll! ch-pong)))
    (is (= {:command :pong}
           (async/poll! ch-pong)))
    
    (is (= {:command :error
            :content {:unknown-msg "STOP\r\n"}}
           (async/poll! ch-unkn)))
    
    (reader/stop-message r)))
