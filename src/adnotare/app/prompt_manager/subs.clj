(ns adnotare.app.prompt-manager.subs
  (:require
   [adnotare.app.context :refer [Context]]
   [adnotare.core.features.manage-prompts :as manage-prompts]
   [adnotare.core.model.palette :as palette :refer [Palette]]
   [adnotare.core.model.prompt :refer [Prompt]]
   [adnotare.core.util.schema :refer [IDSeq IDd]]
   [cljfx.api :as fx]
   [malli.core :as m]))

(defn palettes [ctx]
  (fx/sub-val ctx manage-prompts/palettes))
(m/=> palettes [:=> [:cat Context] (IDSeq Palette)])

(defn selected-palette [ctx]
  (fx/sub-val ctx manage-prompts/selected-palette))
(m/=> selected-palette [:=> [:cat Context] [:maybe (IDd Palette)]])

(defn selected-palette-id [ctx]
  (fx/sub-val ctx manage-prompts/selected-palette-id))

(defn prompts [ctx]
  (if-let [[_id palette] (fx/sub-ctx ctx selected-palette)]
    (palette/ordered-prompts palette)
    []))
(m/=> prompts [:=> [:cat Context] (IDSeq Prompt)])

(defn selected-prompt [ctx]
  (fx/sub-val ctx manage-prompts/selected-prompt))
(m/=> selected-prompt [:=> [:cat Context] [:maybe (IDd Prompt)]])

(defn selected-prompt-id [ctx]
  (fx/sub-val ctx manage-prompts/selected-prompt-id))

(defn draft-palette-label [ctx]
  (fx/sub-val ctx manage-prompts/draft-palette-label))

(defn draft-prompt-label [ctx]
  (fx/sub-val ctx manage-prompts/draft-prompt-label))

(defn draft-prompt-instructions [ctx]
  (fx/sub-val ctx manage-prompts/draft-prompt-instructions))

(defn validation-errors [ctx]
  (fx/sub-val ctx manage-prompts/validation-errors))

(defn invalid-draft? [ctx]
  (fx/sub-val ctx manage-prompts/invalid-draft?))

(defn selected-palette-annotation-count [ctx]
  (if-let [palette-id (fx/sub-ctx ctx selected-palette-id)]
    (fx/sub-val ctx manage-prompts/annotation-count-for-palette palette-id)
    0))

(defn prompt-annotation-count [ctx prompt-id]
  (if-let [palette-id (fx/sub-ctx ctx selected-palette-id)]
    (fx/sub-val ctx manage-prompts/annotation-count-for-prompt palette-id prompt-id)
    0))
