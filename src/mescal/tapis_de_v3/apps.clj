(ns mescal.tapis-de-v3.apps
  (:use [medley.core :only [remove-vals]]
        [mescal.tapis-de-v3.app-listings :only [get-app-name get-app-description]])
  (:require [mescal.tapis-de-v3.constants :as c]
            [mescal.tapis-de-v3.params :as mp]
            [mescal.util :as util]))

(def param-input-mode
  "REQUIRED: Must be provided in a job request.
  FIXED: Defined in the app and not overridable in a job request.
  INCLUDE_ON_DEMAND (default): Included if referenced in a job request.
  INCLUDE_BY_DEFAULT: Included unless include=false in a job request."
  {:required           "REQUIRED"
   :fixed              "FIXED"
   :include-on-demand  "INCLUDE_ON_DEMAND"
   :include-by-default "INCLUDE_BY_DEFAULT"})

(defn- get-boolean
  [value default]
  (cond (nil? value)    default
        (string? value) (Boolean/parseBoolean value)
        :else           value))

(defn- format-group
  [name params]
  (when (some :isVisible params)
    {:step_number 1
     :id          name
     :name        name
     :label       name
     :parameters  params}))

(defn- format-param
  [get-type get-value get-args param]
  (remove-vals nil?
               {:description  (:description param)
                :arguments    (get-args param)
                :defaultValue (get-value param)
                :id           (:name param)
                :isVisible    (and (not= (:inputMode param) (:fixed param-input-mode))
                                   (get-boolean (get-in param [:notes :value :visible]) true))
                :label        (or (get-in param [:notes :details :label])
                                  (:name param))
                :name         (:name param)
                :order        0
                :required     (or (= (:inputMode param) (:required param-input-mode))
                                  (get-boolean (get-in param [:notes :value :required]) false))
                :type         (get-type param)
                :validators   []}))

(defn- param-formatter
  [get-type get-value get-args]
  (fn [param]
    (format-param get-type get-value get-args param)))

(defn- get-default-enum-value
  [{{value-obj :value} :notes}]
  (let [enum-values (util/get-enum-values value-obj)
        default     (:default value-obj)]
    (when-let [default-elem (mp/find-enum-element default enum-values)]
      (mp/format-enum-element default default-elem))))

(defn- get-default-param-value
  [param]
  (let [default (get-in param [:notes :value :default])]
    (case (mp/get-param-type param)
      "TextSelection" (get-default-enum-value param)
      "Flag"          default
      (or (:arg param) default))))

(defn- get-param-args
  [{{value-obj :value} :notes :as param}]
  (let [enum-values (util/get-enum-values value-obj)
        default     (:default value-obj)]
    (if (mp/enum-param? param)
      (map (partial mp/format-enum-element default) enum-values)
      [])))

(defn- input-param-formatter
  [& {:keys [get-default] :or {get-default (constantly "")}}]
  (param-formatter (constantly "FileFolderInput") get-default (constantly [])))

(defn- opt-param-formatter
  [& {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter mp/get-param-type get-default get-param-args))

(defn- output-param-formatter
  [& {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter (constantly "FileOutput") get-default (constantly [])))

(defn- format-params
  [formatter-fn params]
  (map formatter-fn (sort-by #(get-in % [:notes :value :order] 0) params)))

(defn- parse-app-args
  "Sorts Tapis app args, returning them in a list of 3 groups: inputs, params, and outputs.
  Input args come from the `jobAttributes.fileInputs` list, and any params with a matching name
  found inside the `jobAttributes.parameterSet.appArgs` or `jobAttributes.parameterSet.envVariables`
  will take the place of the matching `fileInputs` param in the first group of the results.
  Output args come from the `notes.outputs` field, and just like input params, any params with a
  matching name found inside the `parameterSet` will take the place of the matching output param
  in the last group of the results.
  The remaining `appArgs` and `envVariables` in the `parameterSet` will be returned in the second
  group of the results, except env vars with `keys` matching app arg `names` are excluded and the
  rest are formatted as app args."
  [{:keys [jobAttributes notes]}]
  (let [app-args        (->> jobAttributes :parameterSet :appArgs)
        env-vars        (->> jobAttributes
                             :parameterSet
                             :envVariables
                             (map #(clojure.set/rename-keys % {:key :name, :value :arg})))
        inputs          (:fileInputs jobAttributes)
        outputs         (:outputs notes)
        input-names     (map :name inputs)
        input-name-set  (set input-names)
        arg-names       (map :name app-args)
        arg-names-set   (set arg-names)
        env-names       (remove #(contains? arg-names-set %) (map :name env-vars))
        output-names    (filter #(contains? arg-names-set %) (map :name outputs))
        output-name-set (set output-names)
        all-args        (group-by :name (concat app-args env-vars inputs outputs))
        get-first-arg   (comp first #(get all-args %))
        filtered-params (map get-first-arg
                             (remove #(or (get input-name-set %) (get output-name-set %))
                                     (concat arg-names env-names)))]
    [(map get-first-arg input-names)
     filtered-params
     (map get-first-arg output-names)]))

(defn format-groups
  [app]
  (let [[inputs params outputs] (parse-app-args app)]
    (remove nil?
            [(format-group "Inputs" (format-params (input-param-formatter) inputs))
             (format-group "Parameters" (format-params (opt-param-formatter) params))
             (format-group "Outputs" (format-params (output-param-formatter) outputs))])))

(defn- system-disabled?
  [tapis system-name]
  (not (:enabled (.getSystemInfo tapis system-name))))

(defn format-app
  ([tapis app group-format-fn]
   (let [system-name (:execSystemId (:jobAttributes app))
         app-label   (get-app-name app)
         mod-time    (util/to-utc (:updated app))]
     {:groups           (group-format-fn app)
      :deleted          false
      :disabled         (system-disabled? tapis system-name)
      :label            app-label
      :id               (:id app)
      :name             app-label
      :description      (get-app-description app)
      :integration_date mod-time
      :edited_date      mod-time
      :app_type         c/hpc-app-type
      :system_id        c/hpc-system-id
      :limitChecks      c/limit-checks}))
  ([tapis app]
   (format-app tapis app format-groups)))

(defn load-app-info
  [tapis app-ids]
  (->> (.listApps tapis)
       (filter (comp (set app-ids) :id))
       (map (juxt :id identity))
       (into {})))

(defn format-tool-for-app
  [{:keys [id version containerImage jobType] :as app}]
  {:attribution ""
   :description (get-app-description app)
   :id          id
   :location    containerImage
   :name        id
   :type        jobType
   :version     version})

(defn format-app-details
  [tapis app]
  (let [mod-time (util/to-utc (:updated app))]
    {:integrator_name      c/unknown-value
     :integrator_email     c/unknown-value
     :integration_date     mod-time
     :edited_date          mod-time
     :id                   (:id app)
     :name                 (get-app-name app)
     :references           []
     :description          (get-app-description app)
     :deleted              false
     :disabled             (system-disabled? tapis (:execSystemId (:jobAttributes app)))
     :tools                [(format-tool-for-app app)]
     :categories           [c/hpc-group-overview]
     :app_type             c/hpc-app-type
     :can_favor            false
     :can_rate             false
     :can_run              true
     :is_public            (boolean (:isPublic app))
     :permission           "read"
     :pipeline_eligibility {:is_valid true :reason ""}
     :rating               {:average 0.0 :total 0}
     :step_count           1
     :suggested_categories []
     :system_id            c/hpc-system-id
     :wiki_url             (:helpURI (:notes app))
     :owner                (:owner app)}))

(defn- add-file-info
  [prop]
  (assoc prop
         :format         "Unspecified"
         :retain         false
         :file_info_type "File"))

(defn format-app-tasks
  [app]
  (let [app-name        (get-app-name app)
        select-io-keys  #(select-keys % [:description :format :id :label :name :required])
        format-io-field (comp select-io-keys add-file-info)
        inputs          (map (comp format-io-field (input-param-formatter)) (:inputs app))
        outputs         (map (comp format-io-field (output-param-formatter)) (:outputs app))]
    {:description (get-app-description app)
     :id          (:id app)
     :name        (get-app-name app)
     :system_id   c/hpc-system-id
     :tasks       [{:description (get-app-description app)
                    :system_id   c/hpc-system-id
                    :id          (:id app)
                    :inputs      inputs
                    :name        app-name
                    :outputs     outputs}]}))

(defn- format-rerun-value
  [p v]
  (when p
    (if (mp/enum-param? p)
      (let [enum-values (util/get-enum-values (:value p))]
        (mp/format-enum-element v (mp/find-enum-element v enum-values)))
      v)))

(defn- app-rerun-value-getter
  [job k]
  (let [values (job k)]
    (fn [p]
      (or (format-rerun-value p (values (keyword (:id p))))
          (get-default-param-value p)))))

(defn- format-groups-for-rerun
  [tapis job app]
  (let [[inputs parameters outputs] (parse-app-args app)
        input-getter (comp #(.irodsFilePath tapis %) (app-rerun-value-getter job :inputs))
        format-input (input-param-formatter :get-default input-getter)
        opt-getter   (app-rerun-value-getter job :parameters)
        format-opt   (opt-param-formatter :get-default opt-getter)]
    (remove nil?
            [(format-group "Inputs" (map format-input inputs))
             (format-group "Parameters" (map format-opt parameters))
             (format-group "Outputs" (map (output-param-formatter) outputs))])))

(defn format-app-rerun-info
  [tapis app job]
  (format-app tapis app (partial format-groups-for-rerun tapis job)))

(defn- convert-permissions
  [perms-set]
  (if (contains? perms-set "MODIFY")
    "own"
    "read"))

(defn- format-app-permission
  [{user :username perm-set :permission-set}]
  {:subject    {:id user :source_id "ldap"}
   :permission (convert-permissions perm-set)})

(defn format-app-permissions
  "Formats a Tapis app permissions response for use in the DE."
  [app-id permissions]
  {:system_id   c/hpc-system-id
   :app_id      app-id
   :permissions (map format-app-permission permissions)})
