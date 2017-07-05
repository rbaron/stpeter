(ns stpeter.core
  (:require [aleph.tcp :as tcp]
            [clack.clack :as clack]
            [clojure.core.async :as async]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [manifold.stream :as s])
  (:gen-class))

(def to-esp (async/chan (async/sliding-buffer 1)))

(def help
  (str ":snowflake: *Exemplos de comandos disponíveis*" \newline
       "set ac1 temp 20" \newline
       "set ac2 off" \newline
       "PS.: ac1 é o AC perto da janela da fachada :)"))

(def batta-uid "U025DM3H6")
(def baron-uid "U0SKTLH28")

(defn ack
  [base user]
  (cond (= batta-uid user) (str ":middle-finger: :middle-finger: :middle-finger:")
        (= baron-uid user) (str ":+1:" \newline base)
        :else base))

(defn make-msg
  [msg channel user]
  {:type "message"
   :text (ack msg user)
   :channel channel})

(defn parse-temp
  [str-temp]
  (try
    (Integer. str-temp)
    (catch Exception e
      nil)))

(defn handle-cmd
  [cmd out-chan channel user]
  (if (re-find #"ping" cmd)
    (do (async/go (async/>! to-esp cmd))
        (async/go (async/>! out-chan (make-msg "Sent ping." channel user))))
    (if-let [off-cmd (re-find #"^set ac[12] off$" cmd)]
      (do (async/go (async/>! to-esp cmd))
          (async/go (async/>! out-chan (make-msg "Ok!" channel user))))
      (if-let [[_ temp] (re-find #"^set ac[12] temp (\d+)$" cmd)]
        (let [int-temp (parse-temp temp)]
          (if (and int-temp (>= int-temp 18) (<= int-temp 26))
            (do (async/go (async/>! to-esp cmd))
                (async/go (async/>! out-chan (make-msg "Ok!" channel user))))
            (async/go (async/>! out-chan (make-msg "Temperatura inválida. Use 18 <= temp <= 26" channel user)))))
        (async/go (async/>! out-chan (make-msg help channel user)))))))

(defn handle-msg
  [msg out-chan my-user-id]
  (let [text (get msg :text "")
        pattern (re-pattern (str "<@" my-user-id "> (.*)"))
        matcher (re-matcher pattern text)]
      (if-let [[_ cmd] (re-find matcher)]
        (handle-cmd cmd out-chan (:channel msg) (get msg :user "")))))

(defn ba-to-str
  [ba]
  (apply str (map char ba)))

(defn wait-and-send-to-esp
  [s info]
  (println "Got new connection from esp")
  (async/go-loop []
    (let [[from-ch ch] (async/alts! [to-esp (async/timeout 10000)])
          msg (or from-ch "ping")]
      (let [put-res @(s/try-put! s (str msg \newline) 3000)]
        (if put-res
          (do (println "Successfully put message to esp stream:" true msg)
              (if-let [res @(s/try-take! s 3000)]
                (do (println "Got response from esp:" (ba-to-str res))
                    (recur))
                (println "Couldn't get response from esp. Exiting handler.")))
          (println "Cannot put message to esp (conn probably closed by client)"))))))

(defn handler
  [in-chan out-chan config]
  (async/go-loop []
    (if-let [msg (async/<! in-chan)]
      (do (handle-msg msg out-chan (:my-user-id config))
          (recur))
      (println "Channel is closed"))))

(defn -main
  [& args]
  (tcp/start-server wait-and-send-to-esp {:port 30669})
  (clack/start (env :slack-api-token) handler))
