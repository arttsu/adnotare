(ns adnotare.core.model.annotation
  (:require
   [adnotare.core.model.prompt :refer [Prompt]]
   [adnotare.core.model.prompt-ref :refer [PromptRef]]
   [adnotare.core.model.selection :refer [Selection]]
   [malli.util :as mu]))

(def Annotation
  [:map
   [::prompt-ref PromptRef]
   [::selection Selection]
   [::note :string]])

(def ResolvedAnnotation
  (mu/merge
   Annotation
   [:map
    [::prompt Prompt]
    [::selected? :boolean]]))
