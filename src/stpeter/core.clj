(ns stpeter.core
  (:require [aleph.tcp :as tcp]
            [clack.clack :as clack]
            [clojure.core.async :as async]
            [environ.core :refer [env]]
            [manifold.deferred :as d]
            [manifold.stream :as s])
  (:gen-class))

(defn fast-echo-handler
  [f]
  (fn [s info]
    (s/connect
      (s/map f s)
      s)))

(defn print-msg
  [msg]
  (println "Got message" (String. msg))
  (str "ok" \newline))

(def to-esp (s/stream))

(defn send-ack
  [msg out-chan my-user-id]
  (if (and (= (:type msg) "message")
           (not= (:user my-user-id) my-user-id))
    (async/go (async/>! out-chan {:type "message"
                                  :channel (:channel msg)
                                   :text "Ok!"}))))

(defn handler
  [in-chan out-chan config]
  (async/go-loop []
    (if-let [msg (async/<! in-chan)]
      (do (send-ack msg out-chan (:my-user-id config))
          (recur))
      (println "Channel is closed"))))

(defn -main
  [& args]
  (tcp/start-server (fast-echo-handler print-msg) {:port 10001})
  @(promise))
