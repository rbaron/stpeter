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
       "set temp [17-26]" \newline
       "set ac off"))

(defn handle-cmd
  [cmd out-chan]
  (if-let [off-cmd (re-find #"^set ac off$" cmd)]
    (do (async/go (async/>! to-esp cmd))
        (async/go (async/>! out-chan "Ok!")))
    (if-let [[_ temp] (re-find #"^set temp (\d+)$" cmd)]
      (let [int-temp (Integer. temp)]
        (if (and (>= int-temp 17) (<= int-temp 26))
          (do (async/go (async/>! to-esp cmd))
              (async/go (async/>! out-chan "Ok!")))
          (async/go (async/>! out-chan "Invalid temp. Keep it >= 17 and <= 26"))))
      (async/go (async/>! out-chan (str "Invalid command. " help))))))

(defn handle-msg
  [msg out-chan my-user-id]
  (let [text (get msg :text "")
        pattern (re-pattern (str "<@" my-user-id "> (.*)"))
        matcher (re-matcher pattern text)]
      (if-let [[_ cmd] (re-find matcher)]
        (handle-cmd cmd out-chan))))

(defn wait-and-send-to-esp
  [s info]
  (async/go-loop []
    (if-let [msg (async/<! to-esp)]
      ;(let [put-res @(s/put! s (str (json/write-str msg) \newline))]
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
