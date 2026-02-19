(ns adnotare.core.model.palette
  (:require
   [adnotare.core.model.prompt :as prompt :refer [Prompt]]
   [adnotare.core.util.schema :as schema :refer [Label IDSeq]]
   [adnotare.core.util.uuid :as uuid]
   [malli.core :as m]))

(def Palette
  [:map
   [::label Label]
   [::prompts
    [:map
     [:by-id [:map-of :uuid Prompt]]
     [:order [:sequential :uuid]]]]])

(def default
  {::label "Default"
   ::prompts
   {:by-id
    {(uuid/named "default-prompt-1") {::prompt/label "Comment" ::prompt/instructions "" ::prompt/color 0}
     (uuid/named "default-prompt-2") {::prompt/label "User answer" ::prompt/instructions "" ::prompt/color 1}
     (uuid/named "default-prompt-3") {::prompt/label "Explain" ::prompt/instructions "" ::prompt/color 3}
     (uuid/named "default-prompt-4") {::prompt/label "Give example" ::prompt/instructions "" ::prompt/color 4}
     (uuid/named "default-prompt-5") {::prompt/label "Provide evidence" ::prompt/instructions "" ::prompt/color 7}}
    :order
    [(uuid/named "default-prompt-1")
     (uuid/named "default-prompt-2")
     (uuid/named "default-prompt-3")
     (uuid/named "default-prompt-4")
     (uuid/named "default-prompt-5")]}})

(defn ordered-prompts [palette]
  (map (fn [id] [id (get-in palette [::prompts :by-id id])])
       (get-in palette [::prompts :order])))
(m/=> ordered-prompts [:=> [:cat Palette] (IDSeq Prompt)])
