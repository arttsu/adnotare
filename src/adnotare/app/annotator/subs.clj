(ns adnotare.app.annotator.subs
  (:require
   [adnotare.app.context :refer [Context]]
   [adnotare.core.features.annotate :as annotate]
   [adnotare.core.model.annotation :refer [ResolvedAnnotation]]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.prompt :refer [Prompt]]
   [adnotare.core.util.rich-text :refer [RichText Range]]
   [adnotare.core.util.schema :refer [IDSeq SelectorOptions]]
   [cljfx.api :as fx]
   [malli.core :as m]))

(defn active-prompts [ctx]
  (if-let [[_id palette] (fx/sub-val ctx annotate/active-palette)]
    (palette/ordered-prompts palette)
    []))
(m/=> active-prompts [:=> [:cat Context] (IDSeq Prompt)])

(defn document-rich-text [ctx]
  (fx/sub-val ctx annotate/document-rich-text))
(m/=> document-rich-text [:=> [:cat Context] RichText])

(defn annotations [ctx]
  (fx/sub-val ctx annotate/annotations))
(m/=> annotations [:=> [:cat Context] (IDSeq ResolvedAnnotation)])

(defn any-annotation-selected? [ctx]
  (fx/sub-val ctx annotate/any-annotation-selected?))
(m/=> any-annotation-selected? [:=> [:cat Context] :boolean])

(defn selected-annotation-note [ctx]
  (fx/sub-val ctx annotate/selected-annotation-note))
(m/=> selected-annotation-note [:=> [:cat Context] :string])

(defn selected-annotation-range [ctx]
  (fx/sub-val ctx annotate/selected-annotation-range))
(m/=> selected-annotation-range [:=> [:cat Context] Range])

(defn annotations-as-llm-prompt [ctx]
  (fx/sub-val ctx annotate/annotations-as-llm-prompt))
(m/=> annotations-as-llm-prompt [:=> [:cat Context] :string])

(defn annotations-and-document-as-llm-prompt [ctx]
  (fx/sub-val ctx annotate/annotations-and-document-as-llm-prompt))
(m/=> annotations-and-document-as-llm-prompt [:=> [:cat Context] :string])

(defn palette-selector-options [ctx]
  (fx/sub-val ctx annotate/palette-selector-options))
(m/=> palette-selector-options [:=> [:cat Context] SelectorOptions])
