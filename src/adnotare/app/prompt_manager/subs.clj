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

(defn prompts [ctx]
  (if-let [[_id palette] (fx/sub-ctx ctx selected-palette)]
    (palette/ordered-prompts palette)
    []))
(m/=> prompts [:=> [:cat Context] (IDSeq Prompt)])

(defn selected-prompt [ctx]
  (fx/sub-val ctx manage-prompts/selected-prompt))
(m/=> selected-prompt [:=> [:cat Context] [:maybe (IDd Prompt)]])
