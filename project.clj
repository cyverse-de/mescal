(defproject org.cyverse/mescal "3.0.0"
  :description "A Clojure client library for the Agave API."
  :url "https://github.com/cyverse-de/mescal"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [clj-time "0.7.0"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [medley "0.5.3"]
                 [org.cyverse/authy "2.8.0"]
                 [org.cyverse/clojure-commons "2.8.0"]
                 [org.cyverse/service-logging "2.8.0"]
                 [slingshot "0.10.3"]]
  :eastwood {:exclude-namespaces [:test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :profiles {:repl {:source-paths ["src" "repl/src"]
                    :resource-paths ["repl/resources"]}}
  :plugins [[jonase/eastwood "0.2.3"]
            [test2junit "1.2.2"]])
