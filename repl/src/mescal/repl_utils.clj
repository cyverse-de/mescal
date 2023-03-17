(ns mescal.repl-utils
  (:use [clojure.java.io :only [file]]
        [clojure.java.shell :only [sh]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-time.format :as cf]
            [clojure.string :as string]
            [mescal.core :as mc]
            [mescal.de :as de])
  (:import [java.sql Timestamp]))

(defn- get-tapis-info-file []
  (let [f (file (System/getProperty "user.home") ".tapis" "current")]
    (when-not (.exists f) (throw (IllegalStateException. (str (.getAbsolutePath f) " does not exist"))))
    (when-not (.isFile f) (throw (IllegalStateException. (str (.getAbsolutePath f) " is not a regular file"))))
    (when-not (.canRead f) (throw (IllegalStateException. (str (.getAbsolutePath f) " is not readable"))))
    f))

(def ^:private timestamp-formatter
  (cf/formatter "EEE MMM dd HH:mm:ss zzz yyyy"))

(defn- parse-timestamp [timestamp]
  (Timestamp. (.getMillis (cf/parse timestamp-formatter (string/replace timestamp #"  +" " ")))))

(defn- get-server-info []
  (let [token-info  (json/decode (:out (string/replace (sh "auth-tokens-refresh" "-v") #"^[^\n]+\n" "")))
        server-info (json/decode (slurp (get-tapis-info-file)) true)
        info        (merge token-info server-info)]
    {:api-name       "agave"
     :base-url       (:baseurl info)
     :client-key     (:apikey info)
     :client-secret  (:apisecret info)
     :auth-uri       (str (curl/url (:baseurl info) "oauth2" "authorize"))
     :token-uri      (str (curl/url (:baseurl info) "oauth2" "token"))
     :redirect-uri   ""
     :scope          (:scope info)
     :token-type     (:token_type info)
     :expires-in     (:expires_in info)
     :refresh_token  (:refresh_token info)
     :access-token   (:access_token info)
     :expires-at     (parse-timestamp (:expires_at info))}))

(defn get-de-tapis-client [storage-system & {:keys [jobs-enabled] :or {jobs-enabled true}}]
  (let [server-info (get-server-info)]
    (de/de-tapis-client-v3
     (:base-url server-info)
     storage-system
     (constantly server-info)
     jobs-enabled)))

(defn get-tapis-client [storage-system]
  (let [server-info (get-server-info)]
    (mc/tapis-client-v3
     (:base-url server-info)
     storage-system
     (constantly server-info))))
