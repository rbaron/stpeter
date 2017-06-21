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

(defn handle-msg
  [msg out-chan my-user-id]
  (println "pattern" my-user-id (get msg :text))
  (if (re-matches (re-pattern (str ".*" my-user-id ".*")) (get msg :text ""))
    (async/go
      (async/>! to-esp (:text msg))
      (async/>! out-chan {:type "message"
                          :channel (:channel msg)
                          :text "Ok found my username!"}))))

(defn wait-and-send-to-esp
  [s info]
  (async/go-loop []
    (if-let [msg (async/<! to-esp)]
      (let [put-res @(s/put! s (str (json/write-str msg) \newline))]
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
