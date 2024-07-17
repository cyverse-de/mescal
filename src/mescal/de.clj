(ns mescal.de
  (:require [mescal.core :as core]
            [mescal.tapis-de-v3 :as v3]))

(defprotocol DeTapisClient
  "A Tapis client with customizations that are specific to the discovery environment."
  (hpcAppGroup [_])
  (listApps [_] [_ opts] [_ app-ids opts])
  (emptyAppListing [_])
  (listAppsWithOntology [_ term])
  (searchApps [_ search-term] [_ search-term opts])
  (getApp [_ app-id])
  (getAppDetails [_ app-id])
  (listAppTasks [_ app-id])
  (getAppToolListing [_ app-id])
  (getAppInputIds [_ app-id])
  (hasAppPermission [_ username app-id required-level])
  (listAppPermissions [_ app-ids])
  (shareAppWithUser [_ username app-id level])
  (unshareAppWithUser [_ username app-id])
  (submitJob [_ submission])
  (prepareJobSubmission [_ submission])
  (sendJobSubmission [_ submission])
  (listJobs [_] [_ job-ids])
  (listJob [_ job-id])
  (listJobIds [_])
  (stopJob [_ job-id])
  (getJobHistory [_ job-id])
  (getJobParams [_ job-id])
  (getAppRerunInfo [_ job-id])
  (translateJobStatus [_ status])
  (regenerateJobSubmission [_ job-id])
  (getDefaultOutputName [_ app-id output-id]))

(deftype DeTapisClientV3 [tapis jobs-enabled?]
  DeTapisClient
  (hpcAppGroup [_]
    (v3/hpc-app-group))
  (listApps [_]
    (v3/list-apps tapis jobs-enabled? {}))
  (listApps [_ opts]
    (v3/list-apps tapis jobs-enabled? opts))
  (listApps [_ app-ids opts]
    (v3/list-apps tapis jobs-enabled? app-ids opts))
  (emptyAppListing [_]
    (v3/empty-app-listing))
  (listAppsWithOntology [_ term]
    (v3/list-apps-with-ontology tapis jobs-enabled? term))
  (searchApps [this search-term]
    (.searchApps this search-term {}))
  (searchApps [_ search-term opts]
    (v3/search-apps tapis jobs-enabled? search-term opts))
  (getApp [_ app-id]
    (v3/get-app tapis app-id))
  (getAppDetails [_ app-id]
    (v3/get-app-details tapis app-id))
  (listAppTasks [_ app-id]
    (v3/list-app-tasks tapis app-id))
  (getAppToolListing [_ app-id]
    (v3/get-app-tool-listing tapis app-id))
  (getAppInputIds [_ app-id]
    (v3/get-app-input-ids tapis app-id))
  (hasAppPermission [_ username app-id required-level]
    (v3/has-app-permission tapis username app-id required-level))
  (listAppPermissions [_ app-ids]
    (v3/list-app-permissions tapis app-ids))
  (shareAppWithUser [_ username app-id level]
    (v3/share-app-with-user tapis username app-id level))
  (unshareAppWithUser [_ username app-id]
    (v3/unshare-app-with-user tapis username app-id))
  (submitJob [this submission]
    (->> (.prepareJobSubmission this submission)
         (.sendJobSubmission this)))
  (prepareJobSubmission [_ submission]
    (v3/prepare-job-submission tapis submission))
  (sendJobSubmission [_ submission]
    (v3/send-job-submission tapis submission))
  (listJobs [_]
    (v3/list-jobs tapis jobs-enabled?))
  (listJobs [_ job-ids]
    (v3/list-jobs tapis jobs-enabled? job-ids))
  (listJob [_ job-id]
    (v3/list-job tapis jobs-enabled? job-id))
  (listJobIds [_]
    (mapv :id (.listJobs tapis)))
  (stopJob [_ job-id]
    (.stopJob tapis job-id))
  (getJobHistory [_ job-id]
    (v3/get-job-history tapis job-id))
  (getJobParams [_ job-id]
    (v3/get-job-params tapis job-id))
  (getAppRerunInfo [_ job-id]
    (v3/get-app-rerun-info tapis job-id))
  (translateJobStatus [_ status]
    (v3/translate-job-status status))
  (regenerateJobSubmission [_ job-id]
    (v3/regenerate-job-submission tapis job-id))
  (getDefaultOutputName [_ app-id output-id]
    (v3/get-default-output-name tapis app-id output-id)))

(defn de-tapis-client-v3
  [base-url storage-system token-info-fn jobs-enabled? & tapis-opts]
  (DeTapisClientV3.
   (apply core/tapis-client-v3 base-url storage-system token-info-fn tapis-opts)
   jobs-enabled?))
