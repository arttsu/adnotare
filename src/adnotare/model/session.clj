(ns adnotare.model.session
  (:require
   [adnotare.model.schema :as S]
   [clojure.string :refer [blank?]]
   [malli.core :as m])
  (:import
   (java.util UUID)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;; Private Readers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn- doc-text [session]
  (get-in session [:annotate :doc :text]))

(defn- annotation-id-map [session]
  (get-in session [:annotate :annotations :by-id]))

(defn- annotation-by-id [session id]
  (get (annotation-id-map session) id))

(defn- selected-annotation-id [session]
  (get-in session [:annotate :annotations :selected-id]))

(defn- annotation-selected? [session id]
  (= id (selected-annotation-id session)))

(defn- palette-by-id [session id]
  (get-in session [:palettes :by-id id]))

(defn- active-palette-id [session]
  (get-in session [:annotate :active-palette-id]))

(defn- prompt-by-ref
  ([session {:keys [palette-id prompt-id]}]
   (prompt-by-ref session palette-id prompt-id))
  ([session palette-id prompt-id]
   (get-in session [:palettes :by-id palette-id :prompts :by-id prompt-id])))

(defn- last-used-palette-id [session]
  (let [last-used-ms (get-in session [:palettes :last-used-ms])]
    (if (seq last-used-ms)
      (first (apply max-key val last-used-ms))
      (first (keys (get-in session [:palettes :by-id]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;; Public Readers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn active-palette [session]
  (when-let [id (active-palette-id session)]
    (let [normalized (palette-by-id session id)
          {prompt-id-map :by-id prompt-order :order} (:prompts normalized)
          prompts (map (fn [id] (assoc (get prompt-id-map id) :id id)) prompt-order)]
      (assoc normalized
             :id id
             :prompts prompts))))
(m/=> active-palette [:-> S/Session [:maybe S/Palette]])

(defn annotation-ids [session]
  (keys (annotation-id-map session)))
(m/=> annotation-ids [:-> S/Session [:maybe [:sequential :uuid]]])

(defn annotations [session]
  (->> (annotation-id-map session)
       (mapv (fn [[id annotation]]
               (let [prompt (prompt-by-ref session (:prompt-ref annotation))
                     selected? (annotation-selected? session id)]
                 (assoc annotation :id id :prompt prompt :selected? selected?))))
       (sort-by (comp :start :selection))))
(m/=> annotations [:-> S/Session [:sequential S/Annotation]])

(defn selected-annotation [session]
  (when-let [id (selected-annotation-id session)]
    (let [annotation (annotation-by-id session id)]
      (assoc (annotation-by-id session id)
             :id id
             :prompt (prompt-by-ref session (:prompt-ref annotation))
             :selected? (annotation-selected? session id)))))
(m/=> selected-annotation [:-> S/Session [:maybe S/Annotation]])

(defn doc-rich-text [session]
  (let [spans (map (fn [{:keys [prompt selection selected?]}]
                     (let [style-classes (cond-> ["rich-text-span" (str "color-" (:color prompt))]
                                           selected? (conj "selected"))]
                       {:start (:start selection) :end (:end selection) :style-classes style-classes}))
                   (annotations session))]
    {:text (doc-text session)
     :spans spans}))
(m/=> doc-rich-text [:-> S/Session S/RichTextModel])

(defn annotations-for-llm [session]
  (let [annotations (annotations session)
        sorted (sort-by (comp :start :selection) annotations)]
    (String/join
     "\n"
     (map (fn [{:keys [selection prompt note]}]
            (cond-> (str "<annotation>\n"
                         "<quote>\n"
                         (:text selection) "\n"
                         "</quote>\n"
                         "<prompt>\n"
                         (:text prompt) "\n"
                         "</prompt>\n")
              (not (blank? note)) (str "<note>\n"
                                       note "\n"
                                       "</note>\n")
              true (str "</annotation>\n")))
          sorted))))
(m/=> annotations-for-llm [:-> S/Session :string])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;; Public Transformers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn activate-last-used-palette [session]
  (if-let [palette-id (last-used-palette-id session)]
    (assoc-in session [:annotate :active-palette-id] palette-id)
    session))
(m/=> activate-last-used-palette [:-> S/Session S/Session])

(defn select-annotation [session id]
  (assoc-in session [:annotate :annotations :selected-id] id))
(m/=> select-annotation [:-> S/Session :uuid S/Session])

(defn add-annotation
  ([session prompt-id selection]
   (add-annotation session prompt-id selection UUID/randomUUID))
  ([session prompt-id selection uuid-gen]
   (let [id (uuid-gen)
         palette-id (active-palette-id session)
         annotation {:prompt-ref {:palette-id palette-id :prompt-id prompt-id} :selection selection :note ""}]
     (-> session
         (assoc-in [:annotate :annotations :by-id id] annotation)
         (select-annotation id)))))
(m/=> add-annotation [:function
                      [:-> S/Session :uuid S/Selection S/Session]
                      [:-> S/Session :uuid S/Selection [:-> :uuid] S/Session]])

(defn update-selected-annotation-note [session text]
  (assoc-in session [:annotate :annotations :by-id (selected-annotation-id session) :note] text))
(m/=> update-selected-annotation-note [:-> S/Session :string S/Session])

(defn clear-annotation-selection [session]
  (assoc-in session [:annotate :annotations :selected-id] nil))
(m/=> clear-annotation-selection [:-> S/Session S/Session])

(defn delete-annotation [session id]
  (cond-> (update-in session [:annotate :annotations :by-id] dissoc id)
    (annotation-selected? session id) (clear-annotation-selection)))
(m/=> delete-annotation [:-> S/Session :uuid S/Session])

(defn replace-doc [session text]
  (-> session
      (assoc-in [:annotate :doc :text] text)
      (assoc-in [:annotate :annotations] {:by-id {} :selected-id nil})))
(m/=> replace-doc [:-> S/Session :string S/Session])
