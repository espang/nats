(ns nats.subscription)


;; register a subscription 
;; unsub


(defprotocol ISubscription
  (sub [subject] "subscribe to a subject")
  (unsub [sid] "unsubscribes a subscription"))

(defrecord Subscriptions [state]
  ISubscription
  
  java.io.Closeable
  (close [this]))
