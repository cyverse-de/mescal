(ns mescal.tapis-de-v3.job-params
  (:require [clojure.string :as string]
            [mescal.tapis-de-v3.constants :as c]
            [mescal.tapis-de-v3.params :as mp]
            [mescal.util :as util]))

(defn- format-param-value
  [get-val get-default get-type get-format get-info-type param]
  (let [default   (get-default)
        param-val (get-val)]
    {:data_format      (get-format)
     :full_param_id    (:id param)
     :info_type        (get-info-type)
     :is_default_value (= param-val default)
     :is_visible       (util/get-boolean (get-in param [:value :visible]) false)
     :param_id         (:id param)
     :param_name       (get-in param [:details :label] "")
     :param_type       (get-type param)
     :param_value      {:value param-val}}))

(defn- get-default-param-value
  [param]
  (let [value-obj   (:value param)
        enum-values (util/get-enum-values value-obj)
        default     (first (:default value-obj))]
    (if (mp/enum-param? param)
      (mp/format-enum-element default (mp/find-enum-element default enum-values))
      default)))

(defn- get-param-value
  [param-values param]
  (when-let [param-value (param-values (keyword (:id param)) "")]
    (if (mp/enum-param? param)
      (let [{value-obj :value} param
            enum-values        (util/get-enum-values value-obj)
            default            (first (:default value-obj))]
        (mp/format-enum-element default (mp/find-enum-element param-value enum-values)))
      param-value)))

(defn- get-default-input-value-fn
  [tapis param]
  (fn []
    (let [default-value (get-default-param-value param)]
      (when-not (string/blank? default-value)
        {:path default-value}))))

(defn- format-input-param-value
  [tapis param-values param]
  (format-param-value #(.irodsFilePath tapis (get-param-value param-values param))
                      (get-default-input-value-fn tapis param)
                      (constantly "FileFolderInput")
                      (constantly "Unspecified")
                      (constantly "File")
                      param))

(defn- format-opt-param-value
  [param-values param]
  (format-param-value #(get-param-value param-values param)
                      #(get-default-param-value param)
                      mp/get-param-type
                      (constantly "")
                      (constantly "")
                      param))

(defn format-params
  [tapis job app-id app]
  (let [format-input (partial format-input-param-value tapis (:inputs job))
        format-opt   (partial format-opt-param-value (:parameters job))]
    {:system_id  c/hpc-system-id
     :app_id     app-id
     :parameters (concat (mapv format-input (:inputs app))
                         (mapv format-opt   (:parameters app)))}))
