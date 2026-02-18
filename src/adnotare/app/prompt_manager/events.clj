(ns adnotare.app.prompt-manager.events
  (:require
   [adnotare.app.interface :refer [handle-event]]
   [adnotare.core.features.manage-prompts :as manage-prompts]
   [cljfx.api :as fx]))

(defmethod handle-event :prompt-manager/select-palette
  [{:keys [fx/context id]}]
  {:context (fx/swap-context context manage-prompts/select-palette id)})

(defmethod handle-event :prompt-manager/select-prompt
  [{:keys [fx/context id]}]
  {:context (fx/swap-context context manage-prompts/select-prompt id)})
