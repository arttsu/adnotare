(ns adnotare.app.events
  (:require
   [adnotare.app.interface :refer [handle-event]]
   [adnotare.app.subs :as subs]
   [adnotare.core.features.ui :as ui]
   [adnotare.core.model.app :as app]
   [cljfx.api :as fx])
  (:import
   (javafx.application Platform)))

(defmethod handle-event :app/quit [_]
  (Platform/exit)
  (System/exit 0))

(defmethod handle-event :app/initialize [{:keys [fx/context palettes]}]
  {:context (fx/swap-context context ui/initialize palettes)})

(defmethod handle-event :app/add-toast [{:keys [fx/context id toast]}]
  {:context (fx/swap-context context ui/add-toast id toast)})

(defmethod handle-event :app/clear-toast [{:keys [fx/context id]}]
  {:context (fx/swap-context context ui/clear-toast id)})

(defmethod handle-event :ui/persist-palettes
  [{:keys [fx/context]}]
  {:persist-palettes {:palettes (subs/palettes context)}})

(defmethod handle-event :ui/navigate
  [{:keys [fx/context route]}]
  (let [navigator (case route
                    ::app/annotator ui/goto-annotator
                    ::app/prompt-manager ui/goto-prompt-manager)]
    {:context (fx/swap-context context navigator)}))
