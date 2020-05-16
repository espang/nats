(ns nats.orig
  (:require [clojure.core.async])
  (:import [java.nio.charset StandardCharsets]
           [java.time Duration]
           [io.nats.client Nats]))

(def connection
  (Nats/connect))

(defn publish [conn subject msg]
  (println "send message:" msg)
  (.publish conn 
            subject
            (.getBytes msg StandardCharset/UTF_8)))

(defn subscribe [conn subject]
  (println "subscribe to: " subject)
  (.subscribe conn subject))


