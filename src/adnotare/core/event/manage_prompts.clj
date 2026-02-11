(ns adnotare.core.event.manage-prompts
  (:require
   [adnotare.core.event :as event]
   [adnotare.core.schema :as S]
   [adnotare.core.state.ui.manage-prompts :as state.ui.manage-prompts]
   [malli.core :as m]))

(defn on-select-palette [state palette-id]
  (event/result (state.ui.manage-prompts/select-palette state palette-id)))
(m/=> on-select-palette [:=> [:cat S/State :uuid] S/EventResult])
(defmethod event/handle :manage-prompts/select-palette [state {:keys [palette-id]}]
  (on-select-palette state palette-id))

(defn on-select-prompt [state prompt-id]
  (event/result (state.ui.manage-prompts/select-prompt state prompt-id)))
(m/=> on-select-prompt [:=> [:cat S/State :uuid] S/EventResult])
(defmethod event/handle :manage-prompts/select-prompt [state {:keys [prompt-id]}]
  (on-select-prompt state prompt-id))
