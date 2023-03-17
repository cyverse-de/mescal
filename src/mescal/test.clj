(ns mescal.test
  (:require [authy.core :as authy]
            [cemerick.url :as curl]
            [mescal.core :as mc]
            [mescal.de :as md]))

(defn- get-tapis-base-url []
  (System/getenv "TAPIS_BASE_URL"))

(defn- get-tapis-storage-system []
  (System/getenv "TAPIS_STORAGE_SYSTEM"))

(defn- get-api-key []
  (System/getenv "TAPIS_API_KEY"))

(defn- get-api-secret []
  (System/getenv "TAPIS_API_SECRET"))

(defn- get-username []
  (System/getenv "IPLANT_CAS_SHORT"))

(defn- get-password []
  (System/getenv "IPLANT_CAS_PASS"))

(defn- get-oauth-info [base-url api-key api-secret]
  {:api-name      "tapis"
   :client-key    api-key
   :client-secret api-secret
   :token-uri     (str (curl/url base-url "oauth2" "token"))})

(defn- get-token [base-url api-key api-secret username password]
  (let [oauth-info (get-oauth-info base-url api-key api-secret)]
    (authy/get-access-token-for-credentials oauth-info username password)))

(defn get-test-tapis-client
  ([]
   (get-test-tapis-client {}))
  ([tapis-params]
   (get-test-tapis-client tapis-params (get-username)))
  ([tapis-params username]
   (get-test-tapis-client tapis-params username (get-password)))
  ([tapis-params username password]
   (get-test-tapis-client tapis-params username password (get-api-key) (get-api-secret)))
  ([tapis-params username password api-key api-secret]
   (let [base-url       (get-tapis-base-url)
         storage-system (get-tapis-storage-system)
         token-info     (get-token base-url api-key api-secret username password)
         tapis-params   (flatten (seq tapis-params))]
     (apply mc/tapis-client-v3 base-url storage-system (constantly token-info) tapis-params))))

(defn get-test-de-tapis-client
  ([]
   (get-test-de-tapis-client {}))
  ([tapis-params]
   (get-test-de-tapis-client tapis-params true))
  ([tapis-params jobs-enabled?]
   (get-test-de-tapis-client tapis-params jobs-enabled? (get-username)))
  ([tapis-params jobs-enabled? username]
   (get-test-de-tapis-client tapis-params jobs-enabled? username (get-password)))
  ([tapis-params jobs-enabled? username password]
   (get-test-de-tapis-client tapis-params jobs-enabled? username password (get-api-key) (get-api-secret)))
  ([tapis-params jobs-enabled? username password api-key api-secret]
   (let [base-url       (get-tapis-base-url)
         storage-system (get-tapis-storage-system)
         token-info     (get-token base-url api-key api-secret username password)
         tapis-params   (flatten (seq tapis-params))]
     (apply md/de-tapis-client-v3 base-url storage-system (constantly token-info) jobs-enabled? tapis-params))))
