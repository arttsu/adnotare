(ns adnotare.core.model.prompt-ref
  (:require
   [malli.core :as m]))

(def PromptRef
  [:map
   [::palette-id :uuid]
   [::prompt-id :uuid]])

(defn ->PromptRef [palette-id prompt-id]
  {::palette-id palette-id ::prompt-id prompt-id})
(m/=> ->PromptRef [:=> [:cat :uuid :uuid] PromptRef])
