(ns adnotare.core.features.manage-prompts
  (:require
   [adnotare.core.model.annotation :as annotation]
   [adnotare.core.model.annotator :as annotator]
   [adnotare.core.model.app :as app :refer [App]]
   [adnotare.core.model.document :as document]
   [adnotare.core.model.palette :as palette :refer [Palette]]
   [adnotare.core.model.prompt :as prompt :refer [Prompt]]
   [adnotare.core.model.prompt-manager :as prompt-manager]
   [adnotare.core.model.prompt-ref :as prompt-ref]
   [adnotare.core.util.schema :as schema :refer [IDd IDSeq]]
   [adnotare.core.util.uuid :as uuid]
   [malli.core :as m]))

(defn selected-palette-id [app]
  (get-in app [::app/prompt-manager ::prompt-manager/selected-palette-id]))
(m/=> selected-palette-id [:=> [:cat App] [:maybe :uuid]])

(defn selected-prompt-id [app]
  (get-in app [::app/prompt-manager ::prompt-manager/selected-prompt-id]))
(m/=> selected-prompt-id [:=> [:cat App] [:maybe :uuid]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Accessors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn palettes [app]
  (->> (get-in app [::app/palettes :by-id])
       (sort-by (comp ::palette/label val))))
(m/=> palettes [:=> [:cat App] (IDSeq Palette)])

(defn selected-palette [app]
  (when-let [id (selected-palette-id app)]
    (when-let [palette (get-in app [::app/palettes :by-id id])]
      [id palette])))
(m/=> selected-palette [:=> [:cat App] [:maybe (IDd Palette)]])

(defn selected-prompt [app]
  (when-let [palette-id (selected-palette-id app)]
    (when-let [prompt-id (selected-prompt-id app)]
      (when-let [prompt (app/prompt-by-ref app {::prompt-ref/palette-id palette-id
                                                ::prompt-ref/prompt-id prompt-id})]
        [prompt-id prompt]))))
(m/=> selected-prompt [:=> [:cat App] [:maybe (IDd Prompt)]])

(defn annotation-count-for-palette [app palette-id]
  (->> (get-in app [::app/document ::document/annotations :by-id])
       vals
       (filter (fn [{::annotation/keys [prompt-ref]}]
                 (= palette-id (::prompt-ref/palette-id prompt-ref))))
       count))
(m/=> annotation-count-for-palette [:=> [:cat App :uuid] :int])

(defn annotation-count-for-prompt [app palette-id prompt-id]
  (->> (get-in app [::app/document ::document/annotations :by-id])
       vals
       (filter (fn [{::annotation/keys [prompt-ref]}]
                 (and (= palette-id (::prompt-ref/palette-id prompt-ref))
                      (= prompt-id (::prompt-ref/prompt-id prompt-ref)))))
       count))
(m/=> annotation-count-for-prompt [:=> [:cat App :uuid :uuid] :int])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Draft and validation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn- draft-errors [app]
  (get-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/errors] {}))

(defn validation-errors [app]
  (draft-errors app))
(m/=> validation-errors [:=> [:cat App] [:map-of :keyword :string]])

(defn invalid-draft? [app]
  (not (empty? (draft-errors app))))
(m/=> invalid-draft? [:=> [:cat App] :boolean])

(defn- clear-draft [app]
  (assoc-in app [::app/prompt-manager ::prompt-manager/draft] {::prompt-manager/errors {}}))

(defn- set-error [app field message]
  (assoc-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/errors field] message))

(defn- clear-error [app field]
  (update-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/errors] dissoc field))

(defn- valid-label? [label]
  (m/validate schema/Label label))

(defn- selected-palette-label* [app]
  (some-> (selected-palette app) second ::palette/label))

(defn- selected-prompt-label* [app]
  (some-> (selected-prompt app) second ::prompt/label))

(defn- selected-prompt-instructions* [app]
  (some-> (selected-prompt app) second ::prompt/instructions))

(defn draft-palette-label [app]
  (or (get-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/palette-label])
      (selected-palette-label* app)
      ""))
(m/=> draft-palette-label [:=> [:cat App] :string])

(defn draft-prompt-label [app]
  (or (get-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/prompt-label])
      (selected-prompt-label* app)
      ""))
(m/=> draft-prompt-label [:=> [:cat App] :string])

(defn draft-prompt-instructions [app]
  (or (get-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/prompt-instructions])
      (selected-prompt-instructions* app)
      ""))
(m/=> draft-prompt-instructions [:=> [:cat App] :string])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Raw prompt/palette transformers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn select-palette [app id]
  (-> app
      (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] id)
      (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] nil)
      clear-draft))
(m/=> select-palette [:=> [:cat App :uuid] App])

(defn select-prompt [app id]
  (-> app
      (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] id)
      clear-draft))
(m/=> select-prompt [:=> [:cat App :uuid] App])

(defn add-palette [app]
  (let [palette-id (uuid/random)
        prompt-id (uuid/random)
        new-prompt {::prompt/label "New prompt"
                    ::prompt/instructions ""
                    ::prompt/color 0}
        new-palette {::palette/label "New palette"
                     ::palette/prompts {:by-id {prompt-id new-prompt}
                                        :order [prompt-id]}}]
    (-> app
        (assoc-in [::app/palettes :by-id palette-id] new-palette)
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] palette-id)
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] prompt-id)
        clear-draft)))
(m/=> add-palette [:=> [:cat App] App])

(defn add-prompt [app palette-id]
  (let [prompt-id (uuid/random)
        prompt {::prompt/label "New prompt"
                ::prompt/instructions ""
                ::prompt/color 0}]
    (-> app
        (assoc-in [::app/palettes :by-id palette-id ::palette/prompts :by-id prompt-id] prompt)
        (update-in [::app/palettes :by-id palette-id ::palette/prompts :order] conj prompt-id)
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] prompt-id)
        clear-draft)))
(m/=> add-prompt [:=> [:cat App :uuid] App])

(defn put-palette-label [app palette-id label]
  (assoc-in app [::app/palettes :by-id palette-id ::palette/label] label))
(m/=> put-palette-label [:=> [:cat App :uuid :string] App])

(defn put-prompt-label [app palette-id prompt-id label]
  (assoc-in app [::app/palettes :by-id palette-id ::palette/prompts :by-id prompt-id ::prompt/label] label))
(m/=> put-prompt-label [:=> [:cat App :uuid :uuid :string] App])

(defn put-prompt-instructions [app palette-id prompt-id instructions]
  (assoc-in app [::app/palettes :by-id palette-id ::palette/prompts :by-id prompt-id ::prompt/instructions] instructions))
(m/=> put-prompt-instructions [:=> [:cat App :uuid :uuid :string] App])

(defn put-prompt-color [app palette-id prompt-id color]
  (assoc-in app [::app/palettes :by-id palette-id ::palette/prompts :by-id prompt-id ::prompt/color] color))
(m/=> put-prompt-color [:=> [:cat App :uuid :uuid :int] App])

(defn delete-palette [app palette-id]
  (let [remaining-palettes (dissoc (get-in app [::app/palettes :by-id]) palette-id)]
    (-> app
        (assoc-in [::app/palettes :by-id] remaining-palettes)
        (update-in [::app/palettes :last-used-ms] dissoc palette-id)
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] nil)
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] nil)
        clear-draft)))
(m/=> delete-palette [:=> [:cat App :uuid] App])

(defn delete-prompt [app palette-id prompt-id]
  (-> app
      (update-in [::app/palettes :by-id palette-id ::palette/prompts :by-id] dissoc prompt-id)
      (update-in [::app/palettes :by-id palette-id ::palette/prompts :order]
                 (fn [order] (vec (remove #{prompt-id} order))))
      (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] nil)
      clear-draft))
(m/=> delete-prompt [:=> [:cat App :uuid :uuid] App])

(defn reorder-prompts [app palette-id new-order]
  (assoc-in app [::app/palettes :by-id palette-id ::palette/prompts :order] (vec new-order)))
(m/=> reorder-prompts [:=> [:cat App :uuid [:sequential :uuid]] App])

(defn move-prompt [app palette-id prompt-id direction]
  (let [order (vec (get-in app [::app/palettes :by-id palette-id ::palette/prompts :order]))
        idx (.indexOf order prompt-id)
        target-idx (case direction
                     :up (dec idx)
                     :down (inc idx)
                     idx)]
    (if (or (neg? idx)
            (neg? target-idx)
            (>= target-idx (count order)))
      app
      (let [target-id (nth order target-idx)
            swapped (-> order
                        (assoc idx target-id)
                        (assoc target-idx prompt-id))]
        (reorder-prompts app palette-id swapped)))))
(m/=> move-prompt [:=> [:cat App :uuid :uuid [:enum :up :down]] App])

(defn can-delete-palette? [app]
  (> (count (get-in app [::app/palettes :by-id])) 1))
(m/=> can-delete-palette? [:=> [:cat App] :boolean])

(defn can-delete-prompt? [app palette-id]
  (> (count (get-in app [::app/palettes :by-id palette-id ::palette/prompts :by-id])) 1))
(m/=> can-delete-prompt? [:=> [:cat App :uuid] :boolean])

(defn update-draft-palette-label [app label]
  (let [palette-id (selected-palette-id app)
        app' (assoc-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/palette-label] label)]
    (if (valid-label? label)
      (-> app'
          (clear-error :palette-label)
          (cond-> palette-id (put-palette-label palette-id label)))
      (set-error app' :palette-label "Label cannot be empty"))))
(m/=> update-draft-palette-label [:=> [:cat App :string] App])

(defn update-draft-prompt-label [app label]
  (let [palette-id (selected-palette-id app)
        prompt-id (selected-prompt-id app)
        app' (assoc-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/prompt-label] label)]
    (if (valid-label? label)
      (-> app'
          (clear-error :prompt-label)
          (cond-> (and palette-id prompt-id) (put-prompt-label palette-id prompt-id label)))
      (set-error app' :prompt-label "Label cannot be empty"))))
(m/=> update-draft-prompt-label [:=> [:cat App :string] App])

(defn update-draft-prompt-instructions [app instructions]
  (let [palette-id (selected-palette-id app)
        prompt-id (selected-prompt-id app)
        app' (assoc-in app [::app/prompt-manager ::prompt-manager/draft ::prompt-manager/prompt-instructions] instructions)]
    (cond-> app'
      (and palette-id prompt-id) (put-prompt-instructions palette-id prompt-id instructions))))
(m/=> update-draft-prompt-instructions [:=> [:cat App :string] App])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Orchestration wrappers for prompt-manager flows
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn bump-persist-token [app]
  (update app ::app/persist-token (fnil inc 0)))
(m/=> bump-persist-token [:=> [:cat App] App])

(defn- bump-if-changed [app app']
  (if (= app app') app (bump-persist-token app')))

(defn add-palette* [app]
  (bump-persist-token (add-palette app)))
(m/=> add-palette* [:=> [:cat App] App])

(defn add-prompt* [app palette-id]
  (bump-persist-token (add-prompt app palette-id)))
(m/=> add-prompt* [:=> [:cat App :uuid] App])

(defn update-palette-label* [app label]
  (bump-persist-token (update-draft-palette-label app label)))
(m/=> update-palette-label* [:=> [:cat App :string] App])

(defn update-prompt-label* [app label]
  (bump-persist-token (update-draft-prompt-label app label)))
(m/=> update-prompt-label* [:=> [:cat App :string] App])

(defn update-prompt-instructions* [app instructions]
  (bump-persist-token (update-draft-prompt-instructions app instructions)))
(m/=> update-prompt-instructions* [:=> [:cat App :string] App])

(defn update-prompt-color* [app palette-id prompt-id color]
  (bump-persist-token (put-prompt-color app palette-id prompt-id color)))
(m/=> update-prompt-color* [:=> [:cat App :uuid :uuid :int] App])

(defn move-prompt* [app palette-id prompt-id direction]
  (bump-if-changed app (move-prompt app palette-id prompt-id direction)))
(m/=> move-prompt* [:=> [:cat App :uuid :uuid [:enum :up :down]] App])

(defn purge-annotations-for-palette [app palette-id]
  (let [annotations (get-in app [::app/document ::document/annotations :by-id])
        filtered (into {}
                       (remove (fn [[_id {::annotation/keys [prompt-ref]}]]
                                 (= palette-id (::prompt-ref/palette-id prompt-ref)))
                               annotations))
        app' (assoc-in app [::app/document ::document/annotations :by-id] filtered)
        selected-id (get-in app [::app/annotator ::annotator/selected-annotation-id])]
    (cond-> app'
      (and selected-id (nil? (get filtered selected-id)))
      (assoc-in [::app/annotator ::annotator/selected-annotation-id] nil))))
(m/=> purge-annotations-for-palette [:=> [:cat App :uuid] App])

(defn purge-annotations-for-prompt [app palette-id prompt-id]
  (let [annotations (get-in app [::app/document ::document/annotations :by-id])
        filtered (into {}
                       (remove (fn [[_id {::annotation/keys [prompt-ref]}]]
                                 (and (= palette-id (::prompt-ref/palette-id prompt-ref))
                                      (= prompt-id (::prompt-ref/prompt-id prompt-ref))))
                               annotations))
        app' (assoc-in app [::app/document ::document/annotations :by-id] filtered)
        selected-id (get-in app [::app/annotator ::annotator/selected-annotation-id])]
    (cond-> app'
      (and selected-id (nil? (get filtered selected-id)))
      (assoc-in [::app/annotator ::annotator/selected-annotation-id] nil))))
(m/=> purge-annotations-for-prompt [:=> [:cat App :uuid :uuid] App])

(defn delete-prompt* [app palette-id prompt-id]
  (let [app' (-> app
                 (purge-annotations-for-prompt palette-id prompt-id)
                 (delete-prompt palette-id prompt-id))]
    (bump-if-changed app app')))
(m/=> delete-prompt* [:=> [:cat App :uuid :uuid] App])

(defn delete-palette* [app palette-id]
  (let [active-palette-id (get-in app [::app/annotator ::annotator/active-palette-id])
        app' (-> app
                 (purge-annotations-for-palette palette-id)
                 (delete-palette palette-id))
        remaining (get-in app' [::app/palettes :by-id])
        replacement-id (some->> remaining
                                (sort-by (comp ::palette/label val))
                                ffirst)
        app'' (cond-> app'
                (= active-palette-id palette-id)
                (assoc-in [::app/annotator ::annotator/active-palette-id] replacement-id))]
    (bump-if-changed app app'')))
(m/=> delete-palette* [:=> [:cat App :uuid] App])
