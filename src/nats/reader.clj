(ns nats.reader
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json]
            [clojure.string :as str]))

(defn ^:private parse-info [msg]
  (try
    {:command :info
     :content (json/read-str (subs msg 5 (- (count msg) 3)))}
    (catch Exception e
      {:command :error
       :payload e})))

(defn ^:private parse-ping [msg]
  {:command :ping})

(defn ^:private parse-pong [msg]
  {:command :pong})

(defn ^:private parse-err [msg]
  {:command :nats-error
   :content :not-implemented})

(defn ^:private msg-parser [#^bytes msg]
  (let [smsg (String. msg)]
    (cond
      (str/starts-with? smsg "INFO") (parse-info smsg)
      (str/starts-with? smsg "PING") (parse-ping smsg)
      (str/starts-with? smsg "PONG") (parse-pong smsg)
      (str/starts-with? smsg "-ERR") (parse-err smsg)
      :else {:command :error
             :content {:unknown-msg (if (< 10 (count smsg))
                                      (str (take 10 smsg)
                                           "... message truncated [size="
                                           (count smsg)
                                           "]")
                                      smsg)}})))

;; API

(defn stop-message [{:keys [ch]}]
  (async/close! ch))

(defn get-pub [{:keys [pub]}] pub)

(defn make-reader
  "the reader receives messages on chan-in"
  [chan-in]
  (let [c (async/chan)
        pub (async/pub c :command)
        ch (async/go-loop []
             (let [msg (async/<!! chan-in)]
               (async/>!! c (msg-parser msg))
               (recur)))]
    {:pub pub
     :ch  ch}))
