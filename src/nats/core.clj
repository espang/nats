(ns nats.core
  (:require [clojure.core.async :as async]
            [nats.connection :as nats]))

(defn as-bytes [s]
  (.getBytes s "UTF-8"))

(defn as-str [b]
  (String. b "UTF-8"))

(as-str
 (as-bytes "hello world"))

(defn handler [msg]
  (println "Received a message:" (as-str (:data msg))))

(defn nats-example []
  ; connect to nats
  (with-open [conn (nats/connect)]
    (nats/pub conn "foo" (as-bytes "into the void"))
    
    (with-open [sub (nats/sub conn "foo" handler)]
      (nats/pub conn "foo" (as-bytes "hello foo")))
    ; subscription is gone
    (nats/pub conn "foo" (as-bytes "into the void"))))

(comment
  ;; connect to nats send some messages
  ;; check the checksum 
  (do
    (with-open [conn (nats/connect)]
      (nats/sub conn "foo")
      (time
       (dotimes [n 100000]
         (nats/pub conn "foo" (as-bytes (str "hello foo!" n)))))
      (println "connected - waiting") 
      (Thread/sleep 2500)
      (println "checksum of all messages:" (nats/value conn))
      (println "messages received:" (:msgs @(:state conn)))
      (Thread/sleep 500)
      (println "shuttding down"))
    (println "shut down")))
