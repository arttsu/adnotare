(ns adnotare.core.state.ui.manage-prompts
  (:require
   [adnotare.core.schema :as S]
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [malli.core :as m]))

(defn manage-prompts-ui [state]
  (get-in state [:state/ui :ui/manage-prompts]))
(m/=> manage-prompts-ui [:=> [:cat S/State] S/ManagePromptsUI])

(defn selected-palette-id [state]
  (or (get-in state [:state/ui :ui/manage-prompts :manage-prompts/selected-palette-id])
      (state.ui.annotate/active-palette-id state)))
(m/=> selected-palette-id [:=> [:cat S/State] [:maybe :uuid]])

(defn selected-prompt-id [state]
  (get-in state [:state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id]))
(m/=> selected-prompt-id [:=> [:cat S/State] [:maybe :uuid]])

(defn select-palette [state palette-id]
  (-> state
      (assoc-in [:state/ui :ui/manage-prompts :manage-prompts/selected-palette-id] palette-id)
      (assoc-in [:state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id] nil)))
(m/=> select-palette [:=> [:cat S/State :uuid] S/State])

(defn select-prompt [state prompt-id]
  (assoc-in state [:state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id] prompt-id))
(m/=> select-prompt [:=> [:cat S/State :uuid] S/State])

(defn sync-with-active-palette [state]
  (-> state
      (assoc-in [:state/ui :ui/manage-prompts :manage-prompts/selected-palette-id]
                (state.ui.annotate/active-palette-id state))
      (assoc-in [:state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id] nil)))
(m/=> sync-with-active-palette [:=> [:cat S/State] S/State])
