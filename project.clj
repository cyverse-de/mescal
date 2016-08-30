(defproject org.cyverse/mescal "2.8.0"
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
                 [com.cemerick/url "0.1.1"]
                 [medley "0.5.3"]
                 [org.cyverse/authy "2.8.0"]
                 [org.cyverse/clojure-commons "2.8.0"]
                 [org.cyverse/service-logging "2.8.0"]
                 [slingshot "0.10.3"]])
