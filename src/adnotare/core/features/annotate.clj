(ns adnotare.core.features.annotate
  (:require
   [adnotare.core.model.annotation :as annotation :refer [ResolvedAnnotation]]
   [adnotare.core.model.annotator :as annotator]
   [adnotare.core.model.app :as app :refer [App]]
   [adnotare.core.model.document :as document]
   [adnotare.core.model.palette :as palette :refer [Palette]]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.model.prompt-ref :as prompt-ref]
   [adnotare.core.model.selection :as selection :refer [Selection]]
   [adnotare.core.util.rich-text :as rich-text :refer [Range RichText]]
   [adnotare.core.util.schema :as schema :refer [IDd IDSeq SelectorOptions Millis]]
   [adnotare.core.util.uuid :as uuid]
   [clojure.string :as string]
   [malli.core :as m]))

(defn- selected-annotation-id [app]
  (get-in app [::app/annotator ::annotator/selected-annotation-id]))

(defn- annotation-selected? [app id]
  (= (selected-annotation-id app) id))

(defn- annotation->rich-text-span [app [annotation-id annotation]]
  (let [{::prompt/keys [color]} (app/prompt-by-ref app (::annotation/prompt-ref annotation))
        style-class (cond-> ["rich-text-span" (str "color-" color)]
                      (annotation-selected? app annotation-id) (conj "selected"))
        {::selection/keys [start end]} (::annotation/selection annotation)]
    {:start start :end end :style-class style-class}))

(defn- resolve-annotation [app [id annotation]]
  (let [prompt (app/prompt-by-ref app (::annotation/prompt-ref annotation))
        selected? (annotation-selected? app id)
        resolved (assoc annotation ::annotation/prompt prompt ::annotation/selected? selected?)]
    [id resolved]))

(defn- selected-annotation [app]
  (get-in app [::app/document ::document/annotations :by-id (selected-annotation-id app)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Accessors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn- active-palette-id [app]
  (get-in app [::app/annotator ::annotator/active-palette-id]))

(defn active-palette [app]
  (when-let [id (active-palette-id app)]
    [id (get-in app [::app/palettes :by-id id])]))
(m/=> active-palette [:=> [:cat App] [:maybe (IDd Palette)]])

(defn document-rich-text [app]
  (let [spans (map (partial annotation->rich-text-span app) (document/annotations (::app/document app)))]
    {:text (get-in app [::app/document ::document/text])
     :spans spans}))
(m/=> document-rich-text [:=> [:cat App] RichText])

(defn annotations [app]
  (map (partial resolve-annotation app) (document/annotations (::app/document app))))
(m/=> annotations [:=> [:cat App] (IDSeq ResolvedAnnotation)])

(defn any-annotation-selected? [app]
  (boolean (selected-annotation-id app)))
(m/=> any-annotation-selected? [:=> [:cat App] :boolean])

(defn selected-annotation-note [app]
  (::annotation/note (selected-annotation app)))
(m/=> selected-annotation-note [:=> [:cat App] :string])

(defn selected-annotation-range [app]
  (let [{::selection/keys [start end]} (::annotation/selection (selected-annotation app))]
    {:start start :end end}))
(m/=> selected-annotation-range [:=> [:cat App] Range])

(defn annotations-as-llm-prompt [app]
  (->> (annotations app)
       (map (fn [[_id
                  {::annotation/keys [note]
                   {::selection/keys [quote]} ::annotation/selection
                   prompt ::annotation/prompt}]]
              (let [prompt-text (prompt/effective-text prompt)]
                (cond-> (str "<annotation>\n"
                             "<quote>\n"
                             quote "\n"
                             "</quote>\n"
                             "<prompt>\n"
                             prompt-text "\n"
                             "</prompt>\n")
                  (not (string/blank? note)) (str "<note>\n"
                                                  note "\n"
                                                  "</note>\n")
                  true (str "</annotation>\n")))))
       (string/join "\n")))
(m/=> annotations-as-llm-prompt [:=> [:cat App] :string])

(defn annotations-and-document-as-llm-prompt [app]
  (str "<document>\n"
       (get-in app [::app/document ::document/text]) "\n"
       "</document>\n\n<annotations>\n"
       (annotations-as-llm-prompt app)
       "</annotations>\n"))
(m/=> annotations-and-document-as-llm-prompt [:=> [:cat App] :string])

(defn palette-selector-options [app]
  (let [options (->> (get-in app [::app/palettes :by-id])
                     (map (fn [[id palette]] {:id id :text (::palette/label palette)}))
                     (sort-by :text))
        selected (when-let [[id palette] (active-palette app)]
                   {:id id :text (::palette/label palette)})]
    {:options options, :selected selected}))
(m/=> palette-selector-options [:=> [:cat App] SelectorOptions])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Transformers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn activate-initial-palette [app]
  (let [{:keys [by-id last-used-ms]} (::app/palettes app)
        palette-id
        (when (seq by-id)
          (if (seq last-used-ms)
            (->> last-used-ms (apply max-key val) key)
            (->> by-id (sort-by (comp ::palette/label val)) first key)))]
    (assoc-in app [::app/annotator ::annotator/active-palette-id] palette-id)))
(m/=> activate-initial-palette [:=> [:cat App] App])

(defn add-annotation
  ([app prompt-id selection]
   (add-annotation app prompt-id selection uuid/random))
  ([app prompt-id selection id-gen]
   (let [palette-id (get-in app [::app/annotator ::annotator/active-palette-id])
         prompt-ref (prompt-ref/->PromptRef palette-id prompt-id)
         id (id-gen)
         annotation {::annotation/prompt-ref prompt-ref
                     ::annotation/selection selection
                     ::annotation/note ""}]
     (-> app
         (assoc-in [::app/document ::document/annotations :by-id id] annotation)
         (assoc-in [::app/annotator ::annotator/selected-annotation-id] id)))))
(m/=> add-annotation [:function
                      [:=> [:cat App :uuid Selection] App]
                      [:=> [:cat App :uuid Selection [:=> [:cat] :uuid]] App]])

(defn select-annotation [app id]
  (assoc-in app [::app/annotator ::annotator/selected-annotation-id] id))
(m/=> select-annotation [:=> [:cat App :uuid] App])

(defn delete-annotation [app id]
  (cond-> app
    (annotation-selected? app id) (assoc-in [::app/annotator ::annotator/selected-annotation-id] nil)
    true (update-in [::app/document ::document/annotations :by-id] dissoc id)))
(m/=> delete-annotation [:=> [:cat App :uuid] App])

(defn put-selected-annotation-note [app note]
  (assoc-in app [::app/document ::document/annotations :by-id (selected-annotation-id app) ::annotation/note] note))
(m/=> put-selected-annotation-note [:=> [:cat App :string] App])

(defn put-document-text [app text]
  (-> app
      (assoc-in [::app/document ::document/text] text)
      (assoc-in [::app/document ::document/annotations :by-id] {})
      (assoc-in [::app/annotator ::annotator/selected-annotation-id] nil)))
(m/=> put-document-text [:=> [:cat App :string] App])

(defn switch-palette
  ([app id]
   (switch-palette app id System/currentTimeMillis))
  ([app id clock]
   (-> app
       (assoc-in [::app/annotator ::annotator/active-palette-id] id)
       (assoc-in [::app/palettes :last-used-ms id] (clock)))))
(m/=> switch-palette [:function
                      [:=> [:cat App :uuid] App]
                      [:=> [:cat App :uuid [:=> [:cat] Millis]] App]])
