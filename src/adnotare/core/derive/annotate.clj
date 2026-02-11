(ns adnotare.core.derive.annotate
  (:require
   [adnotare.core.state.ui.annotate :as ui.annotate]
   [clojure.string :as string]))

(defn- prompt-by-ref [state prompt-ref]
  (let [palette-id (:prompt-ref/palette-id prompt-ref)
        prompt-id (:prompt-ref/prompt-id prompt-ref)]
    (get-in state [:state/palettes :palettes/by-id palette-id :palette/prompts :by-id prompt-id])))

(defn annotation [state annotation-id]
  (when-let [normalized (get-in state [:state/document :document/annotations :by-id annotation-id])]
    (assoc normalized
           :annotation/id annotation-id
           :annotation/prompt (prompt-by-ref state (:annotation/prompt-ref normalized))
           :annotation/selected? (= annotation-id (ui.annotate/selected-annotation-id state)))))

(defn annotations [state]
  (->> (keys (get-in state [:state/document :document/annotations :by-id]))
       (map (fn [annotation-id] (annotation state annotation-id)))
       (remove nil?)
       (sort-by (comp :selection/start :annotation/selection))
       vec))

(defn selected-annotation [state]
  (some->> (ui.annotate/selected-annotation-id state)
           (annotation state)))

(defn doc-rich-text [state]
  {:rich-text/text (get-in state [:state/document :document/text])
   :rich-text/spans (mapv (fn [{:annotation/keys [prompt selection selected?]}]
                            (let [base-style ["rich-text-span"
                                              (str "color-" (or (:prompt/color prompt) 0))]]
                              {:span/start (:selection/start selection)
                               :span/end (:selection/end selection)
                               :span/style-classes (cond-> base-style
                                                     selected? (conj "selected"))}))
                          (annotations state))})

(defn annotations-str [state]
  (->> (annotations state)
      (map (fn [{:annotation/keys [prompt selection note]}]
              (cond-> (str "<annotation>\n"
                           "<quote>\n"
                           (:selection/text selection) "\n"
                           "</quote>\n"
                           "<prompt>\n"
                           (or (:prompt/text prompt) "") "\n"
                           "</prompt>\n")
                (not (string/blank? note)) (str "<note>\n"
                                                note "\n"
                                                "</note>\n")
                true (str "</annotation>\n"))))
       (string/join "\n")))
