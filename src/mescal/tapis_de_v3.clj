(ns mescal.tapis-de-v3
  (:use [slingshot.slingshot :only [try+]])
  (:require [mescal.tapis-de-v3.apps :as apps]
            [mescal.tapis-de-v3.app-listings :as app-listings]
            [mescal.tapis-de-v3.job-params :as params]
            [mescal.tapis-de-v3.jobs :as jobs]))

(defn hpc-app-group
  []
  (app-listings/hpc-app-group))

(defn- get-system-statuses
  [tapis]
  (into {} (map (juxt :id :status) (.listSystems tapis))))

(defn list-apps
  ([tapis jobs-enabled? opts]
   (app-listings/list-apps tapis (get-system-statuses tapis) jobs-enabled? opts))
  ([tapis jobs-enabled? app-ids opts]
   (app-listings/list-apps tapis (get-system-statuses tapis) jobs-enabled? app-ids opts)))

(defn empty-app-listing
  []
  (assoc (app-listings/hpc-app-group)
         :apps  []
         :total 0))

(defn list-apps-with-ontology
  [tapis jobs-enabled? term]
  (app-listings/list-apps-with-ontology tapis (get-system-statuses tapis) jobs-enabled? term))

(defn- app-matches?
  [search-term app]
  (some (fn [s] (re-find (re-pattern (str "(?i)\\Q" search-term)) s))
        ((juxt :name :description) app)))

(defn- find-matching-apps
  [tapis jobs-enabled? search-term opts]
  (filter (partial app-matches? search-term)
          (:apps (list-apps tapis jobs-enabled? opts))))

(defn search-apps
  [tapis jobs-enabled? search-term opts]
  (let [matching-apps (find-matching-apps tapis jobs-enabled? search-term opts)]
    {:total (count matching-apps)
     :apps  matching-apps}))

(defn get-app
  [tapis app-id]
  (apps/format-app tapis (.getApp tapis app-id)))

(defn get-app-details
  [tapis app-id]
  (apps/format-app-details tapis (.getApp tapis app-id)))

(defn list-app-tasks
  [tapis app-id]
  (apps/format-app-tasks (.getApp tapis app-id)))

(defn get-app-tool-listing
  [tapis app-id]
  {:tools [(apps/format-tool-for-app (.getApp tapis app-id))]})

(defn get-app-input-ids
  [tapis app-id]
  (mapv :id (:inputs (.getApp tapis app-id))))

(defn list-app-permissions
  [tapis app-ids]
  (map #(apps/format-app-permissions % (.listAppPermissions tapis %)) app-ids))

(defn has-app-permission
  [tapis username app-id required-level]
  (try+
   (let [{:keys [write read execute]} (:permission (.getAppPermission tapis app-id username))]
     (case required-level
       "own"     write
       "write"   write
       "read"    read
       "execute" execute
       false))
   (catch [:status 404] _
     ;; user has no app permissions
     false)
   (catch [:status 501] _
     ;; app is public
     (= required-level "read"))))

(defn share-app-with-user
  [tapis username app-id level]
  (.shareAppWithUser tapis app-id username (apps/format-update-permission level)))

(defn prepare-job-submission
  [tapis submission]
  (jobs/prepare-submission tapis (.getApp tapis (:app_id submission)) submission))

(defn send-job-submission
  [tapis submission]
  (let [app-info (apps/load-app-info tapis [:appId submission])]
    (jobs/format-job-submisison-response tapis submission (.submitJob tapis submission))))

(defn- format-jobs
  [tapis jobs-enabled? jobs]
  (let [app-info (apps/load-app-info tapis (mapv :appId jobs))
        statuses (get-system-statuses tapis)]
    (mapv (partial jobs/format-job tapis jobs-enabled? statuses app-info) jobs)))

(defn list-jobs
  ([tapis jobs-enabled?]
   (format-jobs tapis jobs-enabled? (.listJobs tapis)))
  ([tapis jobs-enabled? job-ids]
   (format-jobs tapis jobs-enabled? (.listJobs tapis job-ids))))

(defn list-job
  [tapis jobs-enabled? job-id]
  (let [job      (.listJob tapis job-id)
        statuses (get-system-statuses tapis)
        app-info (apps/load-app-info tapis [(:appId job)])]
    (jobs/format-job tapis jobs-enabled? statuses app-info job)))

(defn get-job-history
  [tapis job-id]
  (jobs/format-job-history (.getJobHistory tapis job-id)))

(defn get-job-params
  [tapis job-id]
  (when-let [job (.listJob tapis job-id)]
    (params/format-params tapis job (:appId job) (.getApp tapis (:appId job)))))

(defn get-app-rerun-info
  [tapis job-id]
  (when-let [job (.listJob tapis job-id)]
    (apps/format-app-rerun-info tapis (.getApp tapis (:appId job)) job)))

(defn translate-job-status
  [status]
  (jobs/translate-job-status status))

(defn regenerate-job-submission
  [tapis job-id]
  (when-let [job (.listJob tapis job-id)]
    (jobs/regenerate-job-submission tapis job)))

(defn get-default-output-name
  [tapis app-id output-id]
  (some->> (.getApp tapis app-id)
           (:outputs)
           (filter (comp (partial = output-id) :id))
           (first)
           (:value)
           (:default)))
