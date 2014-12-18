(defproject org.iplantc/mescal "4.1.0"
  :description "A Clojure client library for the Agave API."
  :url "https://github.com/iPlantCollaborativeOpenSource/mescal"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.3.1"]
                 [clj-http "1.0.0"]
                 [clj-time "0.7.0"]
                 [com.cemerick/url "0.1.1"]
                 [medley "0.5.3"]
                 [org.iplantc/authy "4.1.0"]
                 [org.iplantc/clojure-commons "4.1.0"]
                 [slingshot "0.10.3"]]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
