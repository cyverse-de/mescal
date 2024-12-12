(ns mescal.util
  (:use [clojure.java.io :only [reader]]
        [medley.core :only [find-first]]
        [slingshot.slingshot :only [throw+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]))

(defn- assert-defined*
  "Ensures that a symbol is non-nil."
  [symbol-name symbol-value]
  (when (nil? symbol-value)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     (str symbol-name " is nil")})))

(defmacro assert-defined
  "Ensures that zero or more symbols are defined."
  [& syms]
  `(do ~@(map (fn [sym] `(@#'assert-defined* ~(name sym) ~sym)) syms)))

(defn decode-json
  "Parses a JSON stream or string."
  [source]
  (if (string? source)
    (cheshire/decode source true)
    (cheshire/decode-stream (reader source) true)))

(def ^:private accepted-timestamp-formats
  ["yyyy-MM-dd'T'HH:mm:ssZZ"
   "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSZZ"])

(def ^:private formatter
  (apply tf/formatter t/utc accepted-timestamp-formats))

(defn to-utc
  "Converts a formatted timestamp to UTC."
  [timestamp]
  (when-not (nil? timestamp)
    (->> (tf/parse formatter timestamp)
         (tf/unparse formatter))))

(defn to-millis
  "Converts a formatted timestamp to milliseconds since the epoch."
  [timestamp]
  (when-not (nil? timestamp)
    (.getMillis (tf/parse formatter timestamp))))

(defn get-boolean
  [value default]
  (cond (nil? value)    default
        (string? value) (Boolean/parseBoolean value)
        :else           value))

(defn find-value
  "Finds the value associated with a key in a map. The first non-nil value associated with one
   of the given keys is returned. With the current implementation, the keys provided must be
   keywords."
  [m ks]
  (find-first (complement nil?) ((apply juxt ks) m)))

(defn get-enum-values
  [value-obj]
  (find-value value-obj [:enumValues :enum_values]))

(defn encode-path
  "Encodes a file path for use in a URL."
  [root-dir file-path]
  (as-> file-path path
    (string/replace path (re-pattern (str "\\Q" root-dir "/")) "")
    (string/split path #"/")
    (map curl/url-encode path)
    (string/join "/" path)))
