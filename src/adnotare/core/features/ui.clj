(ns adnotare.core.features.ui
  (:require
   [adnotare.core.features.annotate :as annotate]
   [adnotare.core.features.manage-prompts :as manage-prompts]
   [adnotare.core.model.app :as app :refer [App]]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.palettes :refer [Palettes]]
   [adnotare.core.model.toast :as toast :refer [Toast]]
   [adnotare.core.util.result :refer [if-ok ReadEDNFileResult]]
   [adnotare.core.util.schema :as schema :refer [IDSeq]]
   [adnotare.core.util.uuid :as uuid]
   [malli.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Accessors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn toasts [app]
  (sort-by (comp ::toast/created-at-ms val) (get-in app [::app/toasts :by-id])))
(m/=> toasts [:=> [:cat App] (IDSeq Toast)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Transformers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn initialize [app persisted-palettes-result]
  (let [default-palettes {:by-id {(uuid/named "default-palette") palette/default} :last-used-ms {}}
        [palettes init-errors]
        (if-ok [persisted-palettes persisted-palettes-result]
               (if (= 1 (:version persisted-palettes))
                 (if (m/validate Palettes (:data persisted-palettes))
                   [(:data persisted-palettes) {}]
                   [default-palettes {::app/read-palettes :invalid-schema}])
                 [default-palettes {::app/read-palettes :unsupported-version}])
               [reason]
               [default-palettes {::app/read-palettes reason}])]
    (prn :init-errors init-errors)
    (-> app
        (assoc ::app/palettes palettes)
        (assoc ::app/init-errors init-errors)
        (annotate/activate-initial-palette)
        (assoc ::app/initialized? true))))
(m/=> initialize [:=> [:cat App ReadEDNFileResult] App])

(defn add-toast [app id toast]
  (assoc-in app [::app/toasts :by-id id] toast))
(m/=> add-toast [:=> [:cat App :uuid Toast] App])

(defn clear-toast [app id]
  (update-in app [::app/toasts :by-id] dissoc id))
(m/=> clear-toast [:=> [:cat App :uuid] App])

(defn goto-annotator [app]
  (assoc app ::app/route ::app/annotator))
(m/=> goto-annotator [:=> [:cat App] App])

(defn goto-prompt-manager [app]
  (let [[active-palette-id _] (annotate/active-palette app)]
    (cond-> app
      true (assoc ::app/route ::app/prompt-manager)
      active-palette-id (manage-prompts/select-palette active-palette-id))))
(m/=> goto-prompt-manager [:=> [:cat App] App])
