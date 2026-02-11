(ns adnotare.app.manage-prompts.subs
  (:require
   [adnotare.core.derive.palettes :as derive.palettes]
   [adnotare.core.state.ui.manage-prompts :as ui.manage-prompts]
   [cljfx.api :as fx]))

(defn palette-options [context]
  (fx/sub-val context derive.palettes/palette-options))

(defn selected-palette-id [context]
  (fx/sub-val context ui.manage-prompts/selected-palette-id))

(defn palette-id [context]
  (fx/sub-ctx context selected-palette-id))

(defn palette [context]
  (fx/sub-val context derive.palettes/manage-prompts-palette))

(defn active-prompts [context]
  (some-> (fx/sub-ctx context palette) :palette/prompts))

(defn selected-prompt-id [context]
  (fx/sub-val context ui.manage-prompts/selected-prompt-id))

(defn selected-prompt [context]
  (fx/sub-val context derive.palettes/manage-prompts-selected-prompt))
