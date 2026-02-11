(ns adnotare.app.manage-prompts.events
  (:require
   [adnotare.core.event :as core.event]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx]))

(defn- apply-core [context event]
  (let [{:keys [state] :as result} (core.event/handle (fx/sub-val context identity) event)]
    (merge {:context (fx/reset-context context state)}
           (dissoc result :state))))

(defmethod handle-event :manage-prompts/select-palette [{:keys [fx/context palette-id]}]
  (apply-core context {:event/type :manage-prompts/select-palette
                       :palette-id palette-id}))

(defmethod handle-event :manage-prompts/select-prompt [{:keys [fx/context prompt-id]}]
  (apply-core context {:event/type :manage-prompts/select-prompt
                       :prompt-id prompt-id}))
