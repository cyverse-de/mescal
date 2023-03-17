(ns mescal.tapis-v3
  (:use [medley.core :only [remove-vals take-upto]]
        [slingshot.slingshot :only [try+ throw+]]
        [service-logging.thread-context :only [set-ext-svc-tag!]])
  (:require [authy.core :as authy]
            [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.error-codes :as ce]
            [mescal.util :as util])
  (:import [java.io IOException]))

;; FIXME Update apps service exception handling when this exception handling is updated
(defn- tapis-unavailable
  [e]
  (let [msg "Tapis appears to be unavailable at this time"]
    (log/error e msg)
    (throw+ {:error_code ce/ERR_UNAVAILABLE
             :reason     msg})))

(defn- refresh-access-token
  [token-info-fn timeout]
  (try+
    (let [new-token-info (authy/refresh-access-token @(token-info-fn) :timeout timeout)]
     (dosync (ref-set (token-info-fn) new-token-info)))
    (catch IOException e
      (tapis-unavailable e))))

(defn- wrap-refresh
  [token-info-fn timeout request-fn]
  (try+
   (request-fn)
   (catch IOException e
     (tapis-unavailable e))
   (catch [:status 503] e
     (tapis-unavailable e))
   (catch [:status 401] _
     (refresh-access-token token-info-fn timeout)
     (request-fn))))

(defmacro ^:private with-refresh
  [[token-info-fn timeout] & body]
  `(wrap-refresh ~token-info-fn ~timeout #(do ~@body)))

(defn check-access-token
  [token-info-fn timeout]
  (when (authy/token-expiring? @(token-info-fn))
    (refresh-access-token token-info-fn timeout)))

(defn- tapis-get*
  [token-info-fn timeout url & [params]]
  (with-refresh [token-info-fn timeout]
    ((comp :result :body)
     (http/get (str url)
               {:oauth-token    (:access-token @(token-info-fn))
                :query-params   (remove-vals nil? (or params {}))
                :as             :json
                :conn-timeout   timeout
                :socket-timeout timeout}))))

(defn- tapis-get-paged
  [token-info-fn timeout page-len url & [params]]
  (->> (iterate (partial + page-len) 0)
       (map (partial assoc (or params {}) :limit page-len :offset))
       (map (partial tapis-get* token-info-fn timeout url))
       (take-upto (comp (partial > page-len) count))
       (apply concat)))

(defn- tapis-get
  [token-info-fn timeout url & [{:keys [page-len] :as params}]]
  (set-ext-svc-tag! "tapis")
  (if page-len
    (tapis-get-paged token-info-fn timeout page-len url (dissoc params :page-len))
    (tapis-get* token-info-fn timeout url params)))

(defn- tapis-post
  [token-info-fn timeout url body]
  (set-ext-svc-tag! "tapis")
  (with-refresh [token-info-fn timeout]
    ((comp :result :body)
     (http/post (str url)
                {:oauth-token   (:access-token @(token-info-fn))
                 :as            :json
                 :accept        :json
                 :content-type  :json
                 :form-params   body}))))

(defn list-systems
  [base-url token-info-fn timeout page-len]
  (tapis-get token-info-fn timeout (curl/url base-url "/v3/systems/") {:page-len page-len}))

(defn get-system-info
  [base-url token-info-fn timeout system-name]
  (tapis-get token-info-fn timeout (curl/url base-url "/v3/systems/" system-name)))

(def ^:private app-listing-fields
  ["id" "label" "name" "version" "lastModified" "executionSystem" "shortDescription" "isPublic" "owner" "available"])

(defn- app-listing-params
  [params]
  (merge (select-keys params [:page-len :id.in :ontology.like])
         {:filter (string/join "," app-listing-fields)}
         (case (:app-subset params)
           :public  {:publicOnly "true"}
           :private {:privateOnly "true"}
           {})))

(defn list-apps
  [base-url token-info-fn timeout params]
  (let [params (app-listing-params params)]
    (tapis-get token-info-fn timeout (curl/url base-url "/v3/apps/") params)))

(defn get-app
  [base-url token-info-fn timeout app-id]
  (tapis-get token-info-fn timeout (curl/url base-url "/v3/apps" app-id)))

(defn list-app-permissions
  [base-url token-info-fn timeout app-id]
  (tapis-get token-info-fn timeout (curl/url base-url "/v3/apps" app-id "pems")))

(defn get-app-permission
  [base-url token-info-fn timeout app-id username]
  (tapis-get token-info-fn timeout (curl/url base-url "/v3/apps" app-id "pems" username)))

(defn share-app-with-user
  [base-url token-info-fn timeout app-id username level]
  (tapis-post token-info-fn timeout (curl/url base-url "/v3/apps" app-id "pems" username) {:permission level}))

(defn submit-job
  [base-url token-info-fn timeout submission]
  (tapis-post token-info-fn timeout (curl/url base-url "/v3/jobs/") submission))

(defn list-jobs
  ([base-url token-info-fn timeout page-len]
   (tapis-get token-info-fn timeout (curl/url base-url "/v3/jobs/") {:page-len page-len}))
  ([base-url token-info-fn timeout page-len job-ids]
   (filter (comp (set job-ids) :id) (list-jobs base-url token-info-fn timeout page-len))))

(defn list-job
  [base-url token-info-fn timeout job-id]
  (tapis-get token-info-fn timeout (curl/url base-url "/v3/jobs" job-id)))

(defn stop-job
  [base-url token-info-fn timeout job-id]
  (tapis-post token-info-fn timeout (curl/url base-url "/v3/jobs" job-id) {:action "stop"}))

(defn get-job-history
  [base-url token-info-fn timeout job-id]
  (tapis-get token-info-fn timeout (curl/url base-url "/v3/jobs" job-id "history")))

(def ^:private root-dir-for
  (memoize (fn [base-url token-info-fn timeout storage-system]
             ((comp :rootDir :storage)
              (get-system-info base-url token-info-fn timeout storage-system)))))

(def ^:private get-default-storage-system
  (memoize (fn [base-url token-info-fn timeout page-len]
             (->> (list-systems base-url token-info-fn timeout page-len)
                  (filter #(and (= (:type %) "STORAGE") (:default %)))
                  (first)
                  (:id)))))

(defn- get-root-dir
  [base-url token-info-fn timeout storage-system]
  (let [root-dir (root-dir-for base-url token-info-fn timeout storage-system)]
    (util/assert-defined root-dir)
    root-dir))

(defn- get-default-root-dir
  [base-url token-info-fn timeout page-len]
  (get-root-dir base-url token-info-fn timeout
                (get-default-storage-system base-url token-info-fn timeout page-len)))

(defn file-path-to-url
  [url-type base-url token-info-fn timeout storage-system file-path]
  (when-not (string/blank? file-path)
    (let [root-dir (get-root-dir base-url token-info-fn timeout storage-system)
          url-path (util/encode-path root-dir file-path)]
      (str (curl/url base-url "/v3/files" url-type "system" storage-system url-path)))))

(defn- build-path
  [base & rest]
  (string/join "/" (concat [(string/replace base #"/+$" "")]
                           (map #(string/replace % #"^/+|/+$" "") rest))))

(defn file-path-to-tapis-url
  [base-url token-info-fn timeout storage-system file-path]
  (when-not (string/blank? file-path)
    (let [root-dir (get-root-dir base-url token-info-fn timeout storage-system)
          url-path (util/encode-path root-dir file-path)]
      (build-path (str "tapis://" storage-system) url-path))))

(defn- files-base
  [base-url]
  (str (curl/url base-url "/v3/files")))

(defn- files-base-regex
  ([base-url]
   (re-pattern (str "\\Q" (files-base base-url) "\\E/[^/]+")))
  ([base-url system-id]
   (re-pattern (str "\\Q" (files-base base-url) "\\E/[^/]+/system/\\Q" system-id))))

(defn- extract-storage-system
  [base-url file-url]
  (let [regex (re-pattern (str "\\Q" base-url "\\E/v3/files/[^/]+/system/([^/]+)"))]
    (second (re-find regex file-url))))

(defn- file-url-to-path
  [base-url token-info-fn timeout page-len file-url]
  (when-not (string/blank? file-url)
    (if-let [storage-system (extract-storage-system base-url file-url)]
      (build-path (get-root-dir base-url token-info-fn timeout storage-system)
                  (curl/url-decode (string/replace file-url (files-base-regex base-url storage-system) "")))
      (build-path (get-default-root-dir base-url token-info-fn timeout page-len)
                  (curl/url-decode (string/replace file-url (files-base-regex base-url) ""))))))

(defn- tapis-url-to-path
  [base-url token-info-fn timeout file-url]
  (when-not (string/blank? file-url)
    (when-let [storage-system (second (re-find #"tapis://([^/]+)" file-url))]
      (build-path (get-root-dir base-url token-info-fn timeout storage-system)
                  (curl/url-decode (string/replace file-url #"tapis://[^/]+" ""))))))

(defn- http-url?
  [url]
  (re-find #"^https?://" url))

(defn- tapis-url?
  [url]
  (re-find #"^tapis://" url))

(defn tapis-to-irods-path
  [base-url token-info-fn timeout page-len storage-system file-url]
  (when-not (string/blank? file-url)
    (cond
      (http-url? file-url)  (file-url-to-path base-url token-info-fn timeout page-len file-url)
      (tapis-url? file-url) (tapis-url-to-path base-url token-info-fn timeout file-url)
      :else                 (-> (get-root-dir base-url token-info-fn timeout storage-system)
                                (build-path file-url)))))

(defn irods-to-tapis-path
  [base-url token-info-fn timeout storage-system irods-path]
  (when-not (string/blank? irods-path)
    (let [root-dir (get-root-dir base-url token-info-fn timeout storage-system)]
      (string/replace irods-path (re-pattern (str "\\Q" root-dir)) ""))))
