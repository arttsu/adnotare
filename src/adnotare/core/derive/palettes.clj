(ns adnotare.core.derive.palettes
  (:require
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [adnotare.core.state.ui.manage-prompts :as state.ui.manage-prompts]
   [clojure.string :as string]))

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

(defn active-palette [state]
  (some->> (state.ui.annotate/active-palette-id state)
           (palette state)))

(defn active-prompts [state]
  (some-> (active-palette state) :palette/prompts))

(defn palette-options [state]
  (->> (get-in state [:state/palettes :palettes/by-id])
       (map (fn [[palette-id palette]]
              {:option/id palette-id
               :option/label (:palette/label palette)}))
       (sort-by (comp string/lower-case :option/label))
       vec))

(defn manage-prompts-selected-palette-id [state]
  (state.ui.manage-prompts/selected-palette-id state))

(defn manage-prompts-palette [state]
  (some->> (manage-prompts-selected-palette-id state)
           (palette state)))

(defn manage-prompts-selected-prompt-id [state]
  (state.ui.manage-prompts/selected-prompt-id state))

(defn manage-prompts-selected-prompt [state]
  (let [prompt-id (manage-prompts-selected-prompt-id state)]
    (some #(when (= (:prompt/id %) prompt-id) %)
          (some-> (manage-prompts-palette state) :palette/prompts))))
