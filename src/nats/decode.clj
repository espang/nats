(ns nats.decode
  (:require [clojure.string :as str]
            [clojure.data.json :as json ])
  (:import [io.netty.buffer ByteBuf ByteBufProcessor]
           [io.netty.util CharsetUtil]))
(byte \P)
;; define the bytes we use as part of our operations:
(def ^:private p (mapv byte [\p \P]))
(def ^:private i (mapv byte [\i \I]))
(def ^:private o (mapv byte [\o \O]))
(def ^:private n (mapv byte [\n \N]))
(def ^:private g (mapv byte [\g \G]))
(def ^:private m (mapv byte [\m \M]))
(def ^:private s (mapv byte [\s \S]))
(def ^:private f (mapv byte [\f \F]))

(def ^:private newline-byte [(byte \newline)])
(def ^:private return [(byte \return)])

(defn ^:private consume-whitespace!
  "Given a ByteBuf expects whitespace and moves the readerIndex
  to the first non whitespace byte. Returns false if the readerIndex
  hasn't changed."
  [^ByteBuf buf]
  (let [next-non-ws-index (.forEachByte
                           buf
                           ByteBufProcessor/FIND_NON_LINEAR_WHITESPACE)]
    (if (< (.readerIndex buf) next-non-ws-index)
      (do 
        (.readerIndex buf next-non-ws-index)
        true)
      false)))

(defn ^:private consume-byte!
  "Return true if the first readable byte
  of the buffer is either b1 or b2.
  Increase the readerIndex of the buffer!"
  [[b1 b2] ^ByteBuf buf]
  (let [b (.readByte buf)]
    (or (= b1 b)
        (= b2 b))))

(defn ^:private is-byte? [[b1 b2] b]
  (or (= b1 b)
      (= b2 b)))

(defn ^:private read-line
  "reads upto a return-newline tuple,
  moves the readerIndex after the linebreak
  and returns the line as a string."
  [^ByteBuf buf]
  (let [newline-index (.forEachByte
                       buf
                       ByteBufProcessor/FIND_LF)
        array         (byte-array (- newline-index ; newline
                                     1             ; return
                                     (.readerIndex buf)))]
    (.readBytes buf array)
    (when (and (consume-byte! return buf)
               (consume-byte! newline-byte buf))
      (String. array "UTF-8"))))

(defn ^:private parse-info
  " i/I is read. Parse the rest of the message."
  [^ByteBuf buf]
  (if (and (consume-byte! n buf)
           (consume-byte! f buf)
           (consume-byte! o buf)
           (consume-whitespace! buf))
    (let [content (read-line buf)]
      (if (some? content)
        {:msg/type :info
         :content  (json/read-str (str/trim content))}
         :protocol-error))
    :protocol-error))

(defn ^:private parse-msg-payload 
  [^ByteBuf buf options]
  (let [number-bytes (Integer/parseInt 
                      (nth options (dec (count options))))
        array        (byte-array number-bytes)]
    (.readBytes buf array)
    (if (and (consume-byte! return buf)
             (consume-byte! newline-byte buf))
      {:msg/type :msg
       :content  {:payload  array
                  :subject  (first options)
                  :sid      (second options)
                  :reply-to (if (= 4 (count options))
                              (options 2)
                              nil)}}
      :protocol-error)))

(defn ^:private parse-msg
  [^ByteBuf buf]
  (if (and (consume-byte! s buf)
           (consume-byte! g buf)
           (consume-whitespace! buf))
    (let [content (read-line buf)]
      (if (some? content)
        (let [options (str/split content #"\s+")]
          (if (< 2 (count options) 5)
            (parse-msg-payload buf options)
            :protocol-error))
        :protocol-error))
    :protocol-error))

(defn ^:private parse-pingpong [^ByteBuf buf]
  (let [next-byte (.readByte buf)
        msg-type  (cond
                    (is-byte? i next-byte) :ping
                    (is-byte? o next-byte) :pong
                    :else nil)]
    (if (nil? msg-type)
      :protocol-error
      (if (and (consume-byte! n buf)
               (consume-byte! g buf)
               (consume-byte! return buf)
               (consume-byte! newline-byte buf))
        {:msg/type msg-type}
        :protocol-error))))

(defn parse [^ByteBuf buf]
  ;; keep track of the reading position
  (.markReaderIndex buf)
  (try
    (let [first-byte (.readByte buf)]
      (cond
        (is-byte? m first-byte) (parse-msg buf)
        (is-byte? p first-byte) (parse-pingpong buf)
        (is-byte? i first-byte) (parse-info buf)
        :else :protocol-error))    
    (catch IndexOutOfBoundsException ex
      ;; couldn't read a complete message. reset reader position
      ;; to re-read the same code.
      (.resetReaderIndex buf)
      nil)))
