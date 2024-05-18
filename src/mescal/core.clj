(ns mescal.core
  (:require [clojure.string :as string]
            [mescal.tapis-v3 :as v3]))

(defprotocol TapisClient
  "A client for the Tapis API."
  (listSystems [_])
  (getSystemInfo [_ system-name])
  (listApps [_] [_ opts] [_ app-ids opts])
  (listAppsWithOntology [_ term])
  (getApp [_ app-id])
  (getAppPermission [_ app-id username])
  (grantAppPermission [_ app-id username levels])
  (revokeAppPermission [_ app-id username levels])
  (listAppShares [_ app-ids])
  (shareAppWithUsers [_ app-id users])
  (unshareAppWithUsers [_ app-id users])
  (submitJob [_ submission])
  (listJobs [_] [_ job-ids])
  (listJob [_ job-id])
  (stopJob [_ job-id])
  (getJobHistory [_ job-id])
  (fileDownloadUrl [_ file-path])
  (fileListingUrl [_ file-path])
  (tapisUrl [_ file-path])
  (irodsFilePath [_ file-url])
  (tapisFilePath [_ file-url])
  (storageSystem [_]))

(deftype TapisClientV3 [base-url storage-system token-info-fn timeout page-len max-query-items]
  TapisClient
  (listSystems [_]
    (v3/check-access-token token-info-fn timeout)
    (v3/list-systems base-url token-info-fn timeout page-len))
  (getSystemInfo [_ system-name]
    (v3/check-access-token token-info-fn timeout)
    (v3/get-system-info base-url token-info-fn timeout system-name))
  (listApps [_]
    (v3/check-access-token token-info-fn timeout)
    (v3/list-apps base-url token-info-fn timeout {:page-len page-len}))
  (listApps [_ opts]
    (v3/check-access-token token-info-fn timeout)
    (v3/list-apps base-url token-info-fn timeout (merge opts {:page-len page-len})))
  (listApps [_ app-ids opts]
    (v3/check-access-token token-info-fn timeout)
    (if (> (count app-ids) max-query-items)
      (v3/list-apps base-url token-info-fn timeout (merge opts {:page-len page-len}))
      (v3/list-apps base-url token-info-fn timeout (merge opts {:page-len page-len
                                                                :id.in    (string/join "," app-ids)}))))
  (listAppsWithOntology [_ term]
    (v3/check-access-token token-info-fn timeout)
    (v3/list-apps base-url token-info-fn timeout {:page-len      page-len
                                                  :ontology.like term}))
  (getApp [_ app-id]
    (v3/check-access-token token-info-fn timeout)
    (v3/get-app base-url token-info-fn timeout app-id))
  (getAppPermission [_ app-id username]
    (v3/check-access-token token-info-fn timeout)
    (v3/get-app-permission base-url token-info-fn timeout app-id username))
  (grantAppPermission [_ app-id username levels]
    (v3/check-access-token token-info-fn timeout)
    (v3/grant-user-app-permissions base-url token-info-fn timeout app-id username levels))
  (revokeAppPermission [_ app-id username levels]
    (v3/check-access-token token-info-fn timeout)
    (v3/revoke-user-app-permissions base-url token-info-fn timeout app-id username levels))
  (listAppShares [_ app-id]
    (v3/check-access-token token-info-fn timeout)
    (v3/list-app-shares base-url token-info-fn timeout app-id))
  (shareAppWithUsers [_ app-id users]
    (v3/check-access-token token-info-fn timeout)
    (v3/share-app-with-users base-url token-info-fn timeout app-id users))
  (unshareAppWithUsers [_ app-id users]
    (v3/check-access-token token-info-fn timeout)
    (v3/unshare-app-with-users base-url token-info-fn timeout app-id users))
  (submitJob [_ submission]
    (v3/check-access-token token-info-fn timeout)
    (v3/submit-job base-url token-info-fn timeout submission))
  (listJobs [_]
    (v3/check-access-token token-info-fn timeout)
    (v3/list-jobs base-url token-info-fn timeout page-len))
  (listJobs [_ job-ids]
    (v3/check-access-token token-info-fn timeout)
    (v3/list-jobs base-url token-info-fn timeout page-len job-ids))
  (listJob [_ job-id]
    (v3/check-access-token token-info-fn timeout)
    (v3/list-job base-url token-info-fn timeout job-id))
  (stopJob [_ job-id]
    (v3/check-access-token token-info-fn timeout)
    (v3/stop-job base-url token-info-fn timeout job-id))
  (getJobHistory [_ job-id]
    (v3/check-access-token token-info-fn timeout)
    (v3/get-job-history base-url token-info-fn timeout job-id))
  (fileDownloadUrl [_ file-path]
    (v3/check-access-token token-info-fn timeout)
    (v3/file-path-to-url "media" base-url token-info-fn timeout storage-system file-path))
  (fileListingUrl [_ file-path]
    (v3/check-access-token token-info-fn timeout)
    (v3/file-path-to-url "listings" base-url token-info-fn timeout storage-system file-path))
  (tapisUrl [_ file-path]
    (v3/check-access-token token-info-fn timeout)
    (v3/file-path-to-tapis-url base-url token-info-fn timeout storage-system file-path))
  (irodsFilePath [_ file-url]
    (v3/check-access-token token-info-fn timeout)
    (v3/tapis-to-irods-path base-url token-info-fn timeout page-len storage-system file-url))
  (tapisFilePath [_ irods-path]
    (v3/check-access-token token-info-fn timeout)
    (v3/irods-to-tapis-path base-url token-info-fn timeout storage-system irods-path))
  (storageSystem [_]
    storage-system))

(defn tapis-client-v3
  [base-url storage-system token-info-fn & {:keys [timeout page-len max-query-items]
                                            :or {timeout         5000
                                                 page-len        100
                                                 max-query-items 50}}]
  (let [token-info-wrapper-fn (memoize #(ref (token-info-fn)))]
    (TapisClientV3. base-url storage-system token-info-wrapper-fn timeout page-len max-query-items)))
