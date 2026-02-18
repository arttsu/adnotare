(ns adnotare.core.model.annotator)

(def Annotator
  [:map
   [::active-palette-id [:maybe :uuid]]
   [::selected-annotation-id [:maybe :uuid]]])

(def base
  {::active-palette-id nil
   ::selected-annotation-id nil})
