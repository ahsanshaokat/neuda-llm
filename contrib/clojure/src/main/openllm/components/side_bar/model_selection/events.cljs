(ns openllm.components.side-bar.model-selection.events
  (:require [openllm.components.side-bar.model-selection.db :as db]
            [openllm.events :refer [check-spec-interceptor]]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-cofx inject-cofx]]
            [openllm.api.http :as api]
            [openllm.api.log4cljs.core :refer [log]]
            [clojure.string :as str])
  (:require-macros [openllm.build :refer [slurp]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;             Coeffects              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(reg-cofx
 ::model-data-json-parsed
 (fn [cofx _]
   (assoc cofx
          :model-data-json-parsed
          (-> "./src/generated/models-data.json"
              (slurp ,) ;; look @ `openllm.build/slurp` to see how this sorcery works
              (js/JSON.parse ,)
              (js->clj , :keywordize-keys true)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;               Events               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sets the `model-type` in the db to the selected model-type. It also sets the
;; `model-id` to the first `model-id` of the selected `model-type`.
;; This event is dispatched by the `model-type` dropdown in the `views.cljs`
;; namespace of this component.
(reg-event-db
 ::set-model-type
 [check-spec-interceptor]
 (fn [db [_ model-type]]
   (assoc-in db (db/key-seq :selected-model :model-type) model-type)))

(reg-event-db
 ::set-model-id
 [check-spec-interceptor]
 (fn [db [_ model-id]]
   (assoc-in db (db/key-seq :selected-model :model-id) model-id)))

;; This event is dispatched by the `initial-db` function in `db.cljs`. It
;; slurps the model data json file and parses it into a clojure map. It then
;; checks if the db already contains data. If it does, it logs a warning and
;; does nothing. If it doesn't, it adds the parsed data to the db.
;; The `models-data.json` file is generated by the `generate-models-data-json`
;; hook in `build.clj` (from JVM Clojure).
(reg-event-fx
 :slurp-model-data-json
 [check-spec-interceptor (inject-cofx ::model-data-json-parsed)]
 (fn [{:keys [db model-data-json-parsed]} _]
   {:db (let [all-models (get db (db/key-seq :all-models))]
          (if (or (= db/loading-text all-models) (nil? all-models))
            (assoc-in db (db/key-seq :all-models) model-data-json-parsed)
            (do (log :warn "Attempted to slurp and parse model data json, but the db already contained data:" all-models)
                db)))}))

(reg-event-fx
 ::log-error
 []
 (fn [_ [_ error]]
   {:fx [(log :error "Error while fetching metadata:" error)]}))

(reg-event-fx
 ::received-metadata
 [check-spec-interceptor]
 (fn [_ [_ metadata]]
   (let [model-type (-> metadata
                        (:model_name ,)
                        (str/replace , "_" "-")
                        (keyword ,))
         model-id (:model_id metadata)]
     {:dispatch-n [[::set-model-type model-type]
                   [::set-model-id model-id]]})))

(reg-event-fx
 :fetch-metadata-endpoint
 []
 (fn [_ _]
   {:dispatch [::api/v1-metadata ""
               {:on-success [::received-metadata]
                :on-failure [::log-error]}]}))
