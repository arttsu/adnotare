(ns adnotare.app.annotate.subs
  (:require
   [adnotare.model.schema :as S]
   [adnotare.model.session :as session]
   [cljfx.api :as fx]
   [malli.core :as m]))

(defn doc-rich-text [context]
  (fx/sub-val context (comp session/doc-rich-text :state/session)))
(m/=> doc-rich-text [:-> S/Context S/RichTextModel])

(defn active-prompts [context]
  (some-> (fx/sub-val context (comp session/active-palette :state/session)) :prompts))
(m/=> active-prompts [:-> S/Context [:maybe [:sequential S/Prompt]]])

(defn annotations [context]
  (fx/sub-val context (comp session/annotations :state/session)))
(m/=> annotations [:-> S/Context [:sequential S/Annotation]])

(defn any-annotations? [context]
  (not (nil? (fx/sub-val context (comp session/annotation-ids :state/session)))))
(m/=> any-annotations? [:-> S/Context :boolean])

(defn selected-annotation [context]
  (fx/sub-val context (comp session/selected-annotation :state/session)))
(m/=> selected-annotation [:-> S/Context [:maybe S/Annotation]])

(defn selected-annotation-note [context]
  (some-> (fx/sub-ctx context selected-annotation) :note))
(m/=> selected-annotation-note [:-> S/Context [:maybe :string]])

(defn selected-annotation-selection [context]
  (some-> (fx/sub-ctx context selected-annotation) :selection))
(m/=> selected-annotation-selection [:-> S/Context [:maybe S/Selection]])

(defn annotations-for-llm [context]
  (fx/sub-val context (comp session/annotations-for-llm :state/session)))
(m/=> annotations-for-llm [:-> S/Context :string])
