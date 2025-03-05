(ns mescal.tapis-de-v3.jobs
  (:use [clojure.java.io :only [file]]
        [medley.core :only [remove-vals]])
  (:require [clojure.string :as string]
            [clojure-commons.file-utils :as file-utils]
            [mescal.tapis-de-v3.app-listings :as app-listings]
            [mescal.tapis-de-v3.constants :as c]
            [mescal.tapis-de-v3.job-params :as params]
            [mescal.tapis-de-v3.params :as mp]
            [mescal.util :as util]
            [me.raynes.fs :as fs]
            [slingshot.slingshot :refer [try+]]))

(defn- add-param-prefix
  [prefix param-name]
  (if-not (string/blank? (str prefix))
    (keyword (str prefix "_" param-name))
    (keyword param-name)))

(defn- format-input-param
  [get-param-val {:keys [name]}]
  (let [v (get-param-val name)]
    {:name name :sourceUrl (when-not (string/blank? v) v)}))

(defn- preprocess-file-param-value
  [v]
  (if-not (string/blank? v)
    (fs/base-name v)
    v))

(defn- preprocess-path-param-value
  [path-prefix v]
  (if-not (string/blank? v)
    (file-utils/path-join path-prefix (preprocess-file-param-value v))
    v))

(defn- format-param
  [get-config-val runtime is-input-name? is-output-name? {:keys [name arg] :as param}]
  (let [param-type (mp/get-param-type param)
        v (get-config-val name)
        v (cond
            (is-input-name? name)  (if (= runtime "DOCKER")
                                     (preprocess-path-param-value "/TapisInput" v)
                                     (preprocess-file-param-value v))
            (is-output-name? name) (if (= runtime "DOCKER")
                                     (preprocess-path-param-value "/TapisOutput" v)
                                     (preprocess-path-param-value "output" v))
            (map? v)               (:value v)
            (= param-type "Flag")  (when v arg)
            :else                  (str v))]
    {:name name :arg (when-not (string/blank? v) v)}))

(defn- format-env-vars
  [env-vars get-config-val runtime is-input-name? is-output-name?]
  (->> env-vars
       (map #(clojure.set/rename-keys % {:key :name, :value :arg}))
       (map (partial format-param get-config-val runtime is-input-name? is-output-name?))
       (map #(clojure.set/rename-keys % {:name :key, :arg :value}))
       (filter :value)))

(defn- format-app-args
  [app-args get-config-val runtime is-input-name? is-output-name?]
  (->> app-args
       (map (partial format-param get-config-val runtime is-input-name? is-output-name?))
       (filter :arg)))

(defn- format-inputs
  [inputs tapis get-config-val]
  (->> inputs
       (map (partial format-input-param (comp #(.tapisUrl tapis %) get-config-val)))
       (filter :sourceUrl)))

(defn- prepare-params
  [tapis {:keys [jobAttributes notes runtime]} param-prefix config]
  (let [app-args        (-> jobAttributes :parameterSet :appArgs)
        env-vars        (-> jobAttributes :parameterSet :envVariables)
        inputs          (:fileInputs jobAttributes)
        outputs         (:outputs notes)
        input-name-set  (set (map :name inputs))
        output-name-set (set (map :name outputs))
        is-input-name?  (partial contains? input-name-set)
        is-output-name? (partial contains? output-name-set)
        get-config-val  (comp config (partial add-param-prefix param-prefix))]
    {:fileInputs   (format-inputs inputs tapis get-config-val)
     :parameterSet {:appArgs      (format-app-args app-args get-config-val runtime is-input-name? is-output-name?)
                    :envVariables (format-env-vars env-vars get-config-val runtime is-input-name? is-output-name?)}}))

(def ^:private submitted "Submitted")
(def ^:private running "Running")
(def ^:private failed "Failed")
(def ^:private completed "Completed")

(def ^:private job-status-translations
  {"ACCEPTED"           submitted
   "PENDING"            submitted
   "STAGING_INPUTS"     submitted
   "CLEANING_UP"        running
   "ARCHIVING"          running
   "STAGING_JOB"        submitted
   "FINISHED"           completed
   "CANCELLED"          completed
   "KILLED"             failed
   "FAILED"             failed
   "STOPPED"            failed
   "RUNNING"            running
   "PAUSED"             running
   "BLOCKED"            running
   "QUEUED"             submitted
   "SUBMITTING_JOB"     submitted
   "STAGED"             submitted
   "PROCESSING_INPUTS"  submitted
   "ARCHIVING_FINISHED" completed
   "ARCHIVING_FAILED"   failed})

(defn- job-notifications
  [callback-url]
  [{:enabled             true
    :eventCategoryFilter "JOB_NEW_STATUS"
    :deliveryTargets     [{:deliveryAddress callback-url
                           :deliveryMethod  "WEBHOOK"}]}])

(defn- build-job-name
  [submission]
  (format "%s_%04d" (:job_id submission) (:step_number submission 1)))

(defn prepare-submission
  [tapis app submission]
  (->> (assoc (prepare-params tapis app (:paramPrefix submission) (:config submission))
              :name              (build-job-name submission)
              :appId             (:app_id submission)
              :appVersion        (:version app)
              :archiveOnAppError true
              :archiveSystemDir  (.tapisFilePath tapis (:output_dir submission))
              :archiveSystemId   (.storageSystem tapis)
              :subscriptions     (job-notifications (:callbackUrl submission))
              :notes             {:appName        (app-listings/get-app-name app)
                                  :appDescription (app-listings/get-app-description app)})
       (remove-vals nil?)))

(defn- app-enabled?
  [statuses jobs-enabled? listing]
  (and jobs-enabled?
       (:enabled listing)
       (statuses (-> listing :jobAttributes :execSystemId))))

(defn- get-result-folder-id
  [tapis job]
  (when-let [tapis-path (:archiveSystemDir job)]
    (.irodsFilePath tapis tapis-path)))

(defn- format-job*
  [tapis app-id app-name app-description job]
  {:id              (str (:uuid job))
   :app_id          app-id
   :app_description app-description
   :app_name        app-name
   :description     ""
   :enddate         (or (util/to-utc (:ended job)) "")
   :system_id       c/hpc-system-id
   :name            (:name job)
   :raw_status      (:status job)
   :resultfolderid  (get-result-folder-id tapis job)
   :startdate       (or (util/to-utc (:created job)) "")
   :status          (job-status-translations (:status job) "")
   :wiki_url        ""})

(defn format-job
  ([tapis jobs-enabled? app-info-map {app-id :appId :as job}]
   (let [app-info (app-info-map app-id {})]
     (format-job* tapis
                  app-id
                  (app-listings/get-app-name app-info)
                  (app-listings/get-app-description app-info)
                  job)))
  ([tapis jobs-enabled? statuses app-info-map {app-id :appId :as job}]
   (let [app-info (app-info-map app-id {})]
     (assoc (format-job tapis jobs-enabled? app-info-map job)
            :app-disabled (not (app-enabled? statuses jobs-enabled? app-info))))))

(defn- decode-job-history-description
  [description]
  (try+
    (-> description util/decode-json :message)
    (catch Exception _
      description)))

(defn format-job-history
  [job-status-updates]
  (for [{:keys [created eventDetail description]} job-status-updates]
    {:status    eventDetail
     :message   (decode-job-history-description description)
     :timestamp (str (util/to-millis created))}))

(defn format-job-submisison-response
  [tapis submission job]
  (format-job* tapis
               (:appId submission)
               (-> submission :notes :appName)
               (-> submission :notes :appDescription)
               job))

(defn translate-job-status
  [status]
  (get job-status-translations status))

(defn regenerate-job-submission
  [tapis job]
  (let [app-id     (:appId job)
        app        (.getApp tapis app-id)
        job-params (:parameters (params/format-params tapis job app-id app))
        cfg-entry  (juxt (comp keyword :param_id) (comp :value :param_value))]
    {:system_id            c/hpc-system-id
     :app_id               app-id
     :name                 (:name job)
     :debug                false
     :notify               false
     :output_dir           (get-result-folder-id tapis job)
     :create_output_subdir true
     :description          ""
     :config               (into {} (map cfg-entry job-params))}))
