(ns adnotare.model.schema
  (:require [malli.util :as mu]))

(def Color [:int {:min 0 :max 9}])
(def Label [:string {:min 1}])
(def Millis [:int {:min 0}])

(def Prompt
  [:map
   [:text [:string {:min 1}]]
   [:color Color]])

(def DenormPrompt
  (mu/merge
   Prompt
   [:map
    [:id :uuid]]))

(def PromptPalette
  [:map
   [:label [:string {:min 1}]]
   [:prompts
    [:map
     [:by-id [:map-of :uuid Prompt]]
     [:order [:sequential :uuid]]]]])

(def PromptRef
  [:map
   [:palette-id :uuid]
   [:prompt-id :uuid]])

(def Selection
  [:map
   [:start [:int {:min 0}]]
   [:end [:int {:min 0}]]
   [:text :string]])

(def Annotation
  [:map
   [:prompt-ref PromptRef]
   [:selection Selection]
   [:note :string]])

(def Session
  [:map
   [:doc [:map [:text :string]]]
   [:annotations
    [:map
     [:by-id [:map-of :uuid Annotation]]
     [:selected-id [:maybe :uuid]]]]
   [:active-palette-id :uuid]])

(def Route [:enum :annotate :manage-prompts])

(def ToastType [:enum :success :warning :error :info])

(def Toast
  [:map
   [:text Label]
   [:type ToastType]
   [:duration-ms Millis]
   [:created-at-ms Millis]])

(def DenormToast
  (mu/merge
   Toast
   [:map
    [:id :uuid]]))

(def State
  [:map
   [:prompt-palettes
    [:map
     [:by-id [:map-of :uuid PromptPalette]]]]
   [:session Session]
   [:ui
    [:map
     [:route Route]
     [:toasts
      [:map
       [:by-id [:map-of :uuid Toast]]]]]]])

(def StyleClass [:string {:min 1}])

(def StyledRichTextSpan
  [:map
   [:start [:int {:min 0}]]
   [:end [:int {:min 0}]]
   [:style-classes [:sequential StyleClass]]])

(def RichTextModel
  [:map
   [:text :string]
   [:spans [:sequential StyledRichTextSpan]]])
