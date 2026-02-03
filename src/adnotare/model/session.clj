(ns adnotare.model.session
  (:require [adnotare.model.schema :as S]
            [adnotare.model.prompt-palettes :as palettes]
            [adnotare.util.uuid :refer [new-uuid]]
            [clojure.string :refer [blank?]]
            [malli.core :as m]))

(defn doc-text [state]
  (get-in state [:session :doc :text]))

(defn annotations-by-id [state]
  (get-in state [:session :annotations :by-id]))

(defn annotation-by-id [state id]
  (get (annotations-by-id state) id))

(defn selected-annotation-id [state]
  (get-in state [:session :annotations :selected-id]))

(defn annotation-selected? [state id]
  (= id (selected-annotation-id state)))

(defn active-palette-id [state]
  (get-in state [:session :active-palette-id]))

(defn- denorm-annotation [state id annotation]
  (let [prompt (palettes/prompt-by-ref state (:prompt-ref annotation))
        selected? (annotation-selected? state id)]
    (assoc annotation
           :id id
           :prompt prompt
           :selected? selected?)))

(defn- denorm-annotation->styled-rich-text-span [{:keys [prompt selection selected?]}]
  (let [style-classes (cond-> ["rich-text-span" (str "color-" (:color prompt))]
                        selected? (conj "selected"))]
    {:start (:start selection) :end (:end selection) :style-classes style-classes}))

(defn annotations [state]
  (mapv (fn [[id annotation]] (denorm-annotation state id annotation)) (annotations-by-id state)))

(defn doc-rich-text [state]
  (let [spans (map denorm-annotation->styled-rich-text-span (annotations state))]
    {:text (doc-text state) :spans spans}))
(m/=> doc-rich-text [:-> S/State S/RichTextModel])

(defn selected-annotation [state]
  (when-let [id (selected-annotation-id state)]
    (annotation-by-id state id)))

;; TODO: Get rid of it and use 'selected-annotation' instead in subs?
(defn selected-annotation-note [state]
  (when-let [annotation (selected-annotation state)]
    (:note annotation)))

(defn- annotation-str [{:keys [selection prompt note]}]
  (cond-> (str "<annotation>\n"
               "  <quote>\n"
               "    " (:text selection) "\n"
               "  </quote>\n"
               "  <prompt>\n"
               "    " (:text prompt) "\n"
               "  </prompt>\n")
    (not (blank? note)) (str "  <note>\n"
                             "    " note "\n"
                             "  </note>\n")
    true (str "</annotation>\n")))

(defn annotations-str [state]
  (let [annotations (annotations state)
        sorted (sort-by (comp :start :selection) annotations)]
    (String/join "\n" (map annotation-str sorted))))
(m/=> annotations-str [:-> S/State :string])

(defn select-annotation [state id]
  (assoc-in state [:session :annotations :selected-id] id))
(m/=> select-annotation [:-> S/State :uuid S/State])

(defn clear-annotation-selection [state]
  (assoc-in state [:session :annotations :selected-id] nil))
(m/=> clear-annotation-selection [:-> S/State S/State])

(defn add-annotation
  ([state prompt-ref selection]
   (add-annotation state prompt-ref selection new-uuid))
  ([state prompt-ref selection uuid-fn]
   (let [id (uuid-fn)
         annotation {:prompt-ref prompt-ref :selection selection :note ""}]
     (-> state
         (assoc-in [:session :annotations :by-id id] annotation)
         (assoc-in [:session :annotations :selected-id] id)))))
(m/=> add-annotation [:function
                      [:-> S/State S/PromptRef S/Selection S/State]
                      [:-> S/State S/PromptRef S/Selection [:-> :uuid] S/State]])

(defn delete-annotation [state id]
  (cond-> (update-in state [:session :annotations :by-id] dissoc id)
    (annotation-selected? state id) (assoc-in [:session :annotations :selected-id] nil)))
(m/=> delete-annotation [:-> S/State :uuid S/State])

(defn replace-doc [state text]
  (-> state
      (assoc-in [:session :doc :text] text)
      (assoc-in [:session :annotations] {:by-id {} :selected-id nil})))
(m/=> replace-doc [:-> S/State :string S/State])

(defn update-selected-annotation-note [state text]
  (assoc-in state [:session :annotations :by-id (selected-annotation-id state) :note] text))
(m/=> update-selected-annotation-note [:-> S/State :string S/State])
