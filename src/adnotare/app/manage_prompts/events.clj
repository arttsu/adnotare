(ns adnotare.app.manage-prompts.events
  (:require
   [adnotare.core.state.ui.manage-prompts :as ui.manage-prompts]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx]))

(defmethod handle-event :manage-prompts/select-palette [{:keys [fx/context palette-id]}]
  (when palette-id
    {:context (fx/swap-context context ui.manage-prompts/select-palette palette-id)}))

(defmethod handle-event :manage-prompts/select-prompt [{:keys [fx/context prompt-id]}]
  (when prompt-id
    {:context (fx/swap-context context ui.manage-prompts/select-prompt prompt-id)}))
