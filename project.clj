(defproject org.cyverse/mescal "3.0.7-SNAPSHOT"
  :description "A Clojure client library for the Agave API."
  :url "https://github.com/cyverse-de/mescal"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [cheshire "5.8.1"]
                 [clj-http "3.9.1"]
                 [clj-time "0.15.1"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [medley "1.1.0"]
                 [org.cyverse/authy "2.8.0"]
                 [org.cyverse/clojure-commons "3.0.3"]
                 [org.cyverse/service-logging "2.8.0"]
                 [slingshot "0.12.2"]]
  :eastwood {:exclude-namespaces [mescal.de :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :profiles {:repl {:source-paths ["src" "repl/src"]
                    :resource-paths ["repl/resources"]}}
  :plugins [[jonase/eastwood "0.3.5"]
            [test2junit "1.2.2"]])
