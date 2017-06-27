(ns stpeter.core
  (:require [aleph.tcp :as tcp]
            [clack.clack :as clack]
            [clojure.core.async :as async]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [manifold.stream :as s])
  (:gen-class))

(def to-esp (async/chan (async/sliding-buffer 1024)))

(def help
  (str "Available commands:" \newline
       "set ac[1-2] temp [18-26]" \newline
       "set ac[1-2] off" \newline
       "PS.: ac1 is the one closer to the front window"))

(defn make-msg
  [msg channel]
  {:type "message"
   :text msg
   :channel channel})

; batta U025DM3H6

(defn handle-cmd
  [cmd out-chan channel]
  (if-let [off-cmd (re-find #"^set ac[12] off$" cmd)]
    (do (async/go (async/>! to-esp cmd))
        (async/go (async/>! out-chan (make-msg "Ok!" channel))))
    (if-let [[_ temp] (re-find #"^set ac[12] temp (\d+)$" cmd)]
      (let [int-temp (Integer. temp)]
        (if (and (>= int-temp 18) (<= int-temp 26))
          (do (async/go (async/>! to-esp cmd))
              (async/go (async/>! out-chan (make-msg "Ok!" channel))))
          (async/go (async/>! out-chan (make-msg "Invalid temp. Keep it >= 18 and <= 26" channel)))))
      (async/go (async/>! out-chan (make-msg (str "Invalid command. " help) channel))))))

(defn handle-msg
  [msg out-chan my-user-id]
  (let [text (get msg :text "")
        pattern (re-pattern (str "<@" my-user-id "> (.*)"))
        matcher (re-matcher pattern text)]
      (if-let [[_ cmd] (re-find matcher)]
        (handle-cmd cmd out-chan (:channel msg)))))

(defn wait-and-send-to-esp
  [s info]
  (async/go-loop []
    (if-let [msg (async/<! to-esp)]
      (let [put-res @(s/put! s (str msg \newline))]
        (if put-res
          (recur)
          (println "Cannot put message to esp (conn probably closed by client)")))
      (println "Cannot take message from to-esp channel (probably closed)"))))

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
