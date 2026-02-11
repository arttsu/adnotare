(ns adnotare.app.manage-prompts.events
  (:require
   [adnotare.fx.handler :refer [handle-event]]
   [adnotare.model.session :as session]
   [cljfx.api :as fx]))

(defmethod handle-event :manage-prompts/select-palette [{:keys [fx/context palette-id]}]
  (when palette-id
    {:context (fx/swap-context context update-in [:state/session] session/select-manage-prompts-palette palette-id)}))

(defmethod handle-event :manage-prompts/select-prompt [{:keys [fx/context prompt-id]}]
  (when prompt-id
    {:context (fx/swap-context context update-in [:state/session] session/select-manage-prompts-prompt prompt-id)}))
