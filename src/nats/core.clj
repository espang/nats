(ns nats.core
  (:require [nats.netty :as netty]))

(defn ^:private start-netty-client [{:keys [host port handler]}]
  (netty/client host port handler))

(defn connect
  ([] (connect {:host "localhost"
                :port 4222}))
  ([options]
   (println "connect to nats")
   (let [netty-state (start-netty-client options)]
     (println "connection to nats established")
     {:tcp-client netty-state
      :state      {}})))

(defn stop [{:keys [tcp-client] :as _state}]
  (println "closing down nats connection")
  (netty/stop tcp-client)
  (println "nats connection closed"))
