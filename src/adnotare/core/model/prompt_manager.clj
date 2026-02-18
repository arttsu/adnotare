(ns adnotare.core.model.prompt-manager)

(def PromptManager
  [:map
   [::selected-palette-id [:maybe :uuid]]
   [::selected-prompt-id [:maybe :uuid]]])

(def base
  {::selected-palette-id nil
   ::selected-prompt-id nil})
