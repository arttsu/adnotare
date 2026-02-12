(ns adnotare.app.manage-prompts.events
  (:require
   [adnotare.core.state.ui.manage-prompts :as state.ui.manage-prompts]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx]))

(defn- apply-state [context state effects]
  (merge {:context (fx/reset-context context state)} effects))

(defmethod handle-event :manage-prompts/select-palette [{:keys [fx/context palette-id]}]
  (let [state (fx/sub-val context identity)
        state (if palette-id
                (state.ui.manage-prompts/select-palette state palette-id)
                state)]
    (apply-state context state {})))

(defmethod handle-event :manage-prompts/select-prompt [{:keys [fx/context prompt-id]}]
  (let [state (fx/sub-val context identity)
        state (if prompt-id
                (state.ui.manage-prompts/select-prompt state prompt-id)
                state)]
    (apply-state context state {})))
