(defproject stpeter "1.0.0"
  :description "Slack bot for controlling the office's AC"
  :url "https://github.com/rbaron/stpeter"
  :license {:name "MIT"}
  :dependencies [
    [org.clojure/clojure "1.8.0"]
    [org.clojure/core.async "0.3.443"]
    [org.clojure/data.json "0.2.6"]
    [aleph "0.4.3"]
    [clack "0.1.0"]
    [manifold "0.1.6"]
  ]
  :main ^:skip-aot stpeter.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
