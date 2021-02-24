(defproject org.cyverse/mescal "3.1.7"
  :description "A Clojure client library for the Agave API."
  :url "https://github.com/cyverse-de/mescal"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :dependencies [[org.clojure/clojure "1.10.2"]
                 [cheshire "5.10.0"]
                 [clj-http "3.11.0"]
                 [clj-time "0.15.2"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [medley "1.3.0"]
                 [org.cyverse/authy "2.8.0"]
                 [org.cyverse/clojure-commons "3.0.6"]
                 [org.cyverse/service-logging "2.8.2"]
                 [slingshot "0.12.2"]]
  :eastwood {:exclude-namespaces [mescal.de :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :profiles {:repl {:source-paths ["src" "repl/src"]
                    :resource-paths ["repl/resources"]}}
  :plugins [[jonase/eastwood "0.3.13"]
            [lein-ancient "0.7.0"]
            [lein-cljfmt "0.6.4"]
            [test2junit "1.2.2"]])
