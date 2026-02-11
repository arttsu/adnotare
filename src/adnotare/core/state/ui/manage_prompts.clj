(ns adnotare.core.state.ui.manage-prompts
  (:require
   [adnotare.core.state.ui.annotate :as ui.annotate]))

(defn manage-prompts-ui [state]
  (get-in state [:state/ui :ui/manage-prompts]))

(defn selected-palette-id [state]
  (or (get-in state [:state/ui :ui/manage-prompts :manage-prompts/selected-palette-id])
      (ui.annotate/active-palette-id state)))

(defn selected-prompt-id [state]
  (get-in state [:state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id]))

(defn select-palette [state palette-id]
  (if (nil? palette-id)
    state
    (-> state
        (assoc-in [:state/ui :ui/manage-prompts :manage-prompts/selected-palette-id] palette-id)
        (assoc-in [:state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id] nil))))

(defn select-prompt [state prompt-id]
  (if (nil? prompt-id)
    state
    (assoc-in state [:state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id] prompt-id)))

(defn sync-with-active-palette [state]
  (-> state
      (assoc-in [:state/ui :ui/manage-prompts :manage-prompts/selected-palette-id]
                (ui.annotate/active-palette-id state))
      (assoc-in [:state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id] nil)))
