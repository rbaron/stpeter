(defproject stpeter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.8.0"]
    [aleph "0.4.3"]
    [clack "0.1.0"]
    [manifold "0.1.6"]
  ]
  :main ^:skip-aot stpeter.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
