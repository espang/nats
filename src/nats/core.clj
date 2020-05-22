(ns nats.core
  (:require [nats.connection :as nats]))



(comment
  (do
    (with-open [conn (nats/connect)]
      (println "connected - waiting")
      (Thread/sleep 5000)
      (println "shuttding down"))
    (println "shut down")))
