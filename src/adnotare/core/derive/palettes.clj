(ns adnotare.core.derive.palettes
  (:require
   [adnotare.core.schema :as S]
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [adnotare.core.state.ui.manage-prompts :as state.ui.manage-prompts]
   [clojure.string :as string]
   [malli.core :as m]))

(defn- prompt [state palette-id prompt-id]
  (when-let [normalized (get-in state [:state/palettes :palettes/by-id palette-id :palette/prompts :by-id prompt-id])]
    (assoc normalized :prompt/id prompt-id)))

(defn palette [state palette-id]
  (when-let [normalized (get-in state [:state/palettes :palettes/by-id palette-id])]
    (let [prompt-order (get-in normalized [:palette/prompts :order])
          prompts (mapv #(prompt state palette-id %) prompt-order)]
      {:palette/id palette-id
       :palette/label (:palette/label normalized)
       :palette/prompts prompts})))
(m/=> palette [:=> [:cat S/State :uuid] [:maybe S/DerivedPalette]])

(defn active-palette [state]
  (some->> (state.ui.annotate/active-palette-id state)
           (palette state)))
(m/=> active-palette [:=> [:cat S/State] [:maybe S/DerivedPalette]])

(defn active-prompts [state]
  (some-> (active-palette state) :palette/prompts))
(m/=> active-prompts [:=> [:cat S/State] [:maybe [:sequential S/DerivedPrompt]]])

(defn palette-options [state]
  (->> (get-in state [:state/palettes :palettes/by-id])
       (map (fn [[palette-id palette]]
              {:option/id palette-id
               :option/label (:palette/label palette)}))
       (sort-by (comp string/lower-case :option/label))
       vec))
(m/=> palette-options [:=> [:cat S/State] [:sequential S/PaletteOption]])

(defn manage-prompts-selected-palette-id [state]
  (state.ui.manage-prompts/selected-palette-id state))
(m/=> manage-prompts-selected-palette-id [:=> [:cat S/State] [:maybe :uuid]])

(defn manage-prompts-palette [state]
  (some->> (manage-prompts-selected-palette-id state)
           (palette state)))
(m/=> manage-prompts-palette [:=> [:cat S/State] [:maybe S/DerivedPalette]])

(defn manage-prompts-selected-prompt-id [state]
  (state.ui.manage-prompts/selected-prompt-id state))
(m/=> manage-prompts-selected-prompt-id [:=> [:cat S/State] [:maybe :uuid]])

(defn manage-prompts-selected-prompt [state]
  (let [prompt-id (manage-prompts-selected-prompt-id state)]
    (some #(when (= (:prompt/id %) prompt-id) %)
          (some-> (manage-prompts-palette state) :palette/prompts))))
(m/=> manage-prompts-selected-prompt [:=> [:cat S/State] [:maybe S/DerivedPrompt]])
