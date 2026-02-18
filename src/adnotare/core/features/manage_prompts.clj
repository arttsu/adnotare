(ns adnotare.core.features.manage-prompts
  (:require
   [adnotare.core.model.app :as app :refer [App]]
   [adnotare.core.model.palette :as palette :refer [Palette]]
   [adnotare.core.model.prompt :as prompt :refer [Prompt]]
   [adnotare.core.model.prompt-manager :as prompt-manager]
   [adnotare.core.model.prompt-ref :as prompt-ref]
   [adnotare.core.util.schema :refer [IDd IDSeq]]
   [malli.core :as m]))

(defn selected-palette-id [app]
  (get-in app [::app/prompt-manager ::prompt-manager/selected-palette-id]))

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
    [id (get-in app [::app/palettes :by-id id])]))
(m/=> selected-palette [:=> [:cat App] [:maybe (IDd Palette)]])

(defn selected-prompt [app]
  (when-let [palette-id (selected-palette-id app)]
    (when-let [prompt-id (get-in app [::app/prompt-manager ::prompt-manager/selected-prompt-id])]
      [prompt-id (app/prompt-by-ref app {::prompt-ref/palette-id palette-id, ::prompt-ref/prompt-id prompt-id})])))
(m/=> selected-prompt [:=> [:cat App] [:maybe (IDd Prompt)]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Transformers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn select-palette [app id]
  (-> app
      (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] id)
      (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] nil)))
(m/=> select-palette [:=> [:cat App :uuid] App])

(defn select-prompt [app id]
  (assoc-in app [::app/prompt-manager ::prompt-manager/selected-prompt-id] id))
(m/=> select-prompt [:=> [:cat App :uuid] App])
