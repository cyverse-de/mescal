(defproject org.cyverse/mescal "4.1.0"
  :description "A Clojure client library for the Tapis API."
  :url "https://github.com/cyverse-de/mescal"
  :license {:name "BSD Standard License"
            :url "https://cyverse.org/license"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :dependencies [[org.clojure/clojure "1.11.3"]
                 [cheshire "5.13.0"]
                 [clj-http "3.13.0"]
                 [clj-time "0.15.2"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [medley "1.4.0"]
                 [me.raynes/fs "1.4.6"]
                 [org.cyverse/authy "3.0.1"]
                 [org.cyverse/clojure-commons "3.0.8"]
                 [org.cyverse/service-logging "2.8.4"]
                 [slingshot "0.12.2"]]
  :eastwood {:exclude-namespaces [mescal.de :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :profiles {:repl {:source-paths ["src" "repl/src"]
                    :resource-paths ["repl/resources"]}}
  :plugins [[jonase/eastwood "1.4.3"]
            [lein-ancient "0.7.0"]
            [lein-cljfmt "0.9.2"]
            [test2junit "1.4.4"]])
