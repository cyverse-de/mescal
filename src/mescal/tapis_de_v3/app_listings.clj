(ns mescal.tapis-de-v3.app-listings
  (:require [mescal.tapis-de-v3.constants :as c]
            [mescal.util :as util]))

(defn hpc-app-group
  []
  {:system_id    c/hpc-system-id
   :id           c/hpc-group-id
   :is_public    true
   :name         c/hpc-group-name
   :total        -1})

(defn get-app-name
  [{:keys [id version notes]}]
  (str (or (:label notes) (:name notes) id) " " version))

(defn get-app-description
  [app]
  (or (:description app) "[no description provided]"))

(defn- format-app-listing
  [statuses jobs-enabled? listing]
  (let [mod-time (util/to-utc (:updated listing))
        system   (:execSystemId (:jobAttributes listing))]
    {:id                   (:id listing)
     :name                 (get-app-name listing)
     :description          (get-app-description listing)
     :integration_date     mod-time
     :edited_date          mod-time
     :app_type             c/hpc-app-type
     :can_favor            false
     :can_rate             false
     :can_run              true
     :deleted              false
     :disabled             (not (and (boolean (:enabled listing))
                                     jobs-enabled?
                                     (statuses system)))
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
     :owner                (:owner listing)
     :limitChecks          c/limit-checks}))

(defn- format-app-listing-response
  [listing statuses jobs-enabled?]
  (assoc (hpc-app-group)
         :apps  (map (partial format-app-listing statuses jobs-enabled?)
                     (remove (comp not boolean :enabled) listing))
         :total (count listing)))

(defn list-apps
  ([tapis statuses jobs-enabled? opts]
   (format-app-listing-response (.listApps tapis opts) statuses jobs-enabled?))
  ([tapis statuses jobs-enabled? app-ids opts]
   (format-app-listing-response (.listApps tapis app-ids opts) statuses jobs-enabled?)))

(defn list-apps-with-ontology
  [tapis statuses jobs-enabled? term]
  (format-app-listing-response (.listAppsWithOntology tapis term) statuses jobs-enabled?))
