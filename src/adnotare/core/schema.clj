(ns adnotare.core.schema
  (:require
   [malli.util :as mu]))

(def Color [:int {:min 0 :max 9}])
(def Label [:string {:min 1}])
(def Millis [:int {:min 0}])

(def Route [:enum :annotate :manage-prompts])
(def ToastType [:enum :success :warning :error :info])

(def Prompt
  [:map
   [:prompt/text [:string {:min 1}]]
   [:prompt/color Color]])

(def NormalizedPrompts
  [:map
   [:by-id [:map-of :uuid Prompt]]
   [:order [:sequential :uuid]]])

(def Palette
  [:map
   [:palette/label Label]
   [:palette/prompts NormalizedPrompts]])

(def Palettes
  [:map
   [:palettes/by-id [:map-of :uuid Palette]]
   [:palettes/last-used-ms [:map-of :uuid Millis]]])

(def Selection
  [:map
   [:selection/start [:int {:min 0}]]
   [:selection/end [:int {:min 0}]]
   [:selection/text :string]])

(def PromptRef
  [:map
   [:prompt-ref/palette-id :uuid]
   [:prompt-ref/prompt-id :uuid]])

(def Annotation
  [:map
   [:annotation/prompt-ref PromptRef]
   [:annotation/selection Selection]
   [:annotation/note :string]])

(def NormalizedAnnotations
  [:map
   [:by-id [:map-of :uuid Annotation]]])

(def Document
  [:map
   [:document/text :string]
   [:document/annotations NormalizedAnnotations]])

(def AnnotateUI
  [:map
   [:annotate/active-palette-id [:maybe :uuid]]
   [:annotate/selected-annotation-id [:maybe :uuid]]])

(def ManagePromptsUI
  [:map
   [:manage-prompts/selected-palette-id [:maybe :uuid]]
   [:manage-prompts/selected-prompt-id [:maybe :uuid]]])

(def NormalizedToast
  [:map
   [:toast/text Label]
   [:toast/type ToastType]
   [:toast/duration-ms Millis]
   [:toast/created-at-ms Millis]])

(def UI
  [:map
   [:ui/initialized? :boolean]
   [:ui/route Route]
   [:ui/toasts [:map [:by-id [:map-of :uuid NormalizedToast]]]]
   [:ui/annotate AnnotateUI]
   [:ui/manage-prompts ManagePromptsUI]])

(def State
  [:map
   [:state/document Document]
   [:state/palettes Palettes]
   [:state/ui UI]])

(def StyledRichTextSpan
  [:map
   [:span/start [:int {:min 0}]]
   [:span/end [:int {:min 0}]]
   [:span/style-classes [:sequential Label]]])

(def RichTextModel
  [:map
   [:rich-text/text :string]
   [:rich-text/spans [:sequential StyledRichTextSpan]]])

(def DerivedPrompt
  (mu/merge Prompt [:map [:prompt/id :uuid]]))

(def DerivedPalette
  [:map
   [:palette/id :uuid]
   [:palette/label Label]
   [:palette/prompts [:sequential DerivedPrompt]]])

(def DerivedAnnotation
  (mu/merge
   Annotation
   [:map
    [:annotation/id :uuid]
    [:annotation/prompt [:maybe Prompt]]
    [:annotation/selected? :boolean]]))

(def PersistedPalettes
  [:map
   [:palettes/version [:= 1]]
   [:palettes/data Palettes]])
