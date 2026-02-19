(ns adnotare.core.model.prompt-manager)

(def PromptManagerDraft
  [:map
   [::palette-label {:optional true} [:maybe :string]]
   [::prompt-label {:optional true} [:maybe :string]]
   [::prompt-instructions {:optional true} [:maybe :string]]
   [::errors {:optional true} [:map-of :keyword :string]]])

(def PromptManager
  [:map
   [::selected-palette-id [:maybe :uuid]]
   [::selected-prompt-id [:maybe :uuid]]
   [::draft PromptManagerDraft]])

(def base
  {::selected-palette-id nil
   ::selected-prompt-id nil
   ::draft {::errors {}}})
