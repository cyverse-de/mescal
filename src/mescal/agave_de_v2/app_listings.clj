(ns mescal.agave-de-v2.app-listings
  (:require [mescal.agave-de-v2.constants :as c]
            [mescal.util :as util]))

(defn hpc-app-group
  []
  {:system_id    c/hpc-system-id
   :id           c/hpc-group-id
   :is_public    true
   :name         c/hpc-group-name
   :total        -1})

(defn get-app-name
  [app]
  (str (or (:label app) (:name app)) " " (:version app)))

(defn- format-app-listing
  [statuses jobs-enabled? listing]
  (let [mod-time (util/to-utc (:lastModified listing))
        system   (:executionSystem listing)]
    {:id                   (:id listing)
     :name                 (get-app-name listing)
     :description          (or (:shortDescription listing) "[no description provided]")
     :integration_date     mod-time
     :edited_date          mod-time
     :app_type             c/hpc-app-type
     :can_favor            false
     :can_rate             false
     :can_run              true
     :deleted              false
     :disabled             (not (and jobs-enabled? (= "UP" (statuses system))))
     :system_id            c/hpc-system-id
     :integrator_email     c/unknown-value
     :integrator_name      c/unknown-value
     :is_favorite          false
     :is_public            (boolean (:isPublic listing))
     :pipeline_eligibility {:is_valid true :reason ""}
     :rating               {:average 0.0 :total 0}
     :step_count           1
     :permission           "read"
     :wiki_url             ""
     :owner                (:owner listing)}))

(defn- format-app-listing-response
  [listing statuses jobs-enabled?]
  (assoc (hpc-app-group)
    :apps  (map (partial format-app-listing statuses jobs-enabled?) listing)
    :total (count listing)))

(defn list-apps
  ([agave statuses jobs-enabled? opts]
     (format-app-listing-response (.listApps agave opts) statuses jobs-enabled?))
  ([agave statuses jobs-enabled? app-ids opts]
     (format-app-listing-response (.listApps agave app-ids opts) statuses jobs-enabled?)))

(defn list-apps-with-ontology
  [agave statuses jobs-enabled? term]
  (format-app-listing-response (.listAppsWithOntology agave term) statuses jobs-enabled?))
