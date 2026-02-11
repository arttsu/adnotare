(ns adnotare.model.schema
  (:require
   [malli.util :as mu]))

(def Color [:int {:min 0 :max 9}])
(def Label [:string {:min 1}])
(def Millis [:int {:min 0}])
(def ResultStatus [:enum :ok :error])

;; TODO: Improve validation:
;; - start < end
;; - style class is a list of names (identifiers)
;; - validate that start/end < text length (on RichTextModel)
(def StyledRichTextSpan
  [:map
   [:start [:int {:min 0}]]
   [:end [:int {:min 0}]]
   [:style-classes [:sequential Label]]])

(def RichTextModel
  [:map
   [:text :string]
   [:spans [:sequential StyledRichTextSpan]]])

(def HasID [:map
            [:id :uuid]])

(def Result
  [:map
   [:status ResultStatus]
   [:reason {:optional true} :string]])

(def Option
  [:map
   [:id :uuid]
   [:label Label]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;; State
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(def NormalizedPrompt
  [:map
   [:text [:string {:min 1}]]
   [:color Color]])

(def Prompt
  (mu/merge NormalizedPrompt HasID))

(def NormalizedPalette
  [:map
   [:label [:string {:min 1}]]
   [:prompts
    [:map
     [:by-id [:map-of :uuid NormalizedPrompt]]
     [:order [:sequential :uuid]]]]])

(def Palette
  (mu/merge
   NormalizedPalette
   [:map
    [:id :uuid]
    [:prompts [:sequential Prompt]]]))

(def PromptRef
  [:map
   [:palette-id :uuid]
   [:prompt-id :uuid]])

(def Selection
  [:map
   [:start [:int {:min 0}]]
   [:end [:int {:min 0}]]
   [:text :string]])

(def NormalizedAnnotation
  [:map
   [:prompt-ref PromptRef]
   [:selection Selection]
   [:note :string]])

(def Annotation
  (mu/merge
   NormalizedAnnotation
   [:map
    [:id :uuid]
    [:prompt NormalizedPrompt]
    [:selected? :boolean]]))

(def Annotate
  [:map
   [:doc [:map [:text :string]]]
   [:annotations
    [:map
     [:by-id [:map-of :uuid NormalizedAnnotation]]
     [:selected-id [:maybe :uuid]]]]
   [:active-palette-id [:maybe :uuid]]])

(def Palettes
  [:map
   [:by-id [:map-of :uuid NormalizedPalette]]
   [:last-used-ms [:map-of :uuid Millis]]])

(def PersistedSession
  [:map
   [:palettes Palettes]])

(def ManagePrompts
  [:map
   [:selected-palette-id [:maybe :uuid]]
   [:selected-prompt-id [:maybe :uuid]]])

(def Session
  [:map
   [:palettes Palettes]
   [:annotate Annotate]
   [:manage-prompts ManagePrompts]])

(def Route [:enum :annotate :manage-prompts])

(def ToastType [:enum :success :warning :error :info])

(def NormalizedToast
  [:map
   [:text Label]
   [:type ToastType]
   [:duration-ms Millis]
   [:created-at-ms Millis]])

(def Toast
  (mu/merge NormalizedToast HasID))

(def App
  [:map
   [:initialized? :boolean]
   [:route Route]
   [:toasts
    [:map
     [:by-id [:map-of :uuid NormalizedToast]]]]])

(def State
  [:map
   [:state/session Session]
   [:state/app App]])

(def Context
  [:map
   [:cljfx.context/m State]])

(def InitStateResult
  (mu/merge
   Result
   [:map
    [:state State]]))
