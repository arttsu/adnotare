(ns adnotare.core.model.app
  (:require
   [adnotare.core.model.annotator :as annotator :refer [Annotator]]
   [adnotare.core.model.document :as document :refer [Document]]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.palettes :as palettes :refer [Palettes]]
   [adnotare.core.model.prompt :as prompt :refer [Prompt]]
   [adnotare.core.model.prompt-manager :as prompt-manager :refer [PromptManager]]
   [adnotare.core.model.prompt-ref :as prompt-ref :refer [PromptRef]]
   [adnotare.core.model.toast :refer [Toast]]
   [adnotare.core.util.schema :refer [ReadVersionedEDNFileError]]
   [malli.core :as m]))

(def InitErrors
  [:map
   [::read-palettes {:optional true} ReadVersionedEDNFileError]])

(def Route [:enum ::annotator ::prompt-manager])

(def App
  [:map
   [::initialized? :boolean]
   [::init-errors InitErrors]
   [::route Route]
   [::toasts
    [:map
     [:by-id [:map-of :uuid Toast]]]]
   [::palettes Palettes]
   [::document Document]
   [::annotator Annotator]
   [::prompt-manager PromptManager]])

(def base
  {::initialized? false
   ::init-errors {}
   ::route ::annotator
   ::toasts {:by-id {}}
   ::palettes palettes/base
   ::document document/base
   ::annotator annotator/base
   ::prompt-manager prompt-manager/base})

(defn prompt-by-ref [app {::prompt-ref/keys [palette-id prompt-id]}]
  (get-in app [::palettes :by-id palette-id ::palette/prompts :by-id prompt-id]))
(m/=> prompt-by-ref [:=> [:cat App PromptRef] Prompt])
