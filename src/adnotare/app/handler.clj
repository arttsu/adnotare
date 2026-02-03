(ns adnotare.app.handler
  (:require [cljfx.api :as fx]
            [adnotare.fx.handler :refer [handle-event]]
            [adnotare.app.events]
            [adnotare.app.annotate.events]
            [adnotare.model.constants :refer [default-state]]
            [adnotare.app.node-registry :as node-registry]
            [adnotare.util.uuid :refer [new-uuid]])
  (:import (javafx.application Platform)
           (javafx.scene.control Alert Alert$AlertType ButtonType)
           (javafx.scene.input Clipboard ClipboardContent)
           (javafx.animation PauseTransition)
           (javafx.util Duration)
           (javafx.event EventHandler)))

(def *state
  (atom (fx/create-context default-state)))

(defn- run-later! [f]
  (if (Platform/isFxApplicationThread)
    (f)
    (Platform/runLater f)))

(def event-handler
  (-> handle-event
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context (fx/make-reset-effect *state)
        :dispatch fx/dispatch-effect
        :confirm
        (fn [{:keys [title header content yes-event]} dispatch]
          (let [alert (doto (Alert. Alert$AlertType/CONFIRMATION)
                        (.setTitle title)
                        (.setHeaderText header)
                        (.setContentText content))
                result (.orElse (.showAndWait alert) ButtonType/CANCEL)]
            (when (= result ButtonType/OK)
              (dispatch yes-event))))
        :toast
        (fn [{:keys [toast]} dispatch]
          (let [id (new-uuid)]
            (doto (PauseTransition. (Duration/millis (:duration-ms toast)))
              (.setOnFinished (reify EventHandler
                                (handle [_ _]
                                  (dispatch {:event/type :app/clear-toast
                                             :id id}))))
              (.play))
            (dispatch {:event/type :app/add-toast
                       :id id
                       :toast toast})))
        :consume-event
        (fn [event _dispatch]
          (.consume event))
        :get-selection
        (fn [{:keys [node-key on-selection]} dispatch]
          (when-let [selection (node-registry/get-selection node-key)]
            (dispatch (assoc on-selection :selection selection))))
        :get-clipboard
        (fn [{:keys [on-clipboard]} dispatch]
          (let [clipboard (Clipboard/getSystemClipboard)
                text (when (.hasString clipboard) (.getString clipboard))]
            (dispatch (assoc on-clipboard :text text))))
        :copy-to-clipboard
        (fn [{:keys [text]} _dispatch]
          (let [clipboard (Clipboard/getSystemClipboard)
                content (doto (ClipboardContent.)
                          (.putString text))]
            (.setContent clipboard content)))
        :ui
        (fn [{:keys [updates]} _dispatch]
          ;; Yield.
          (run-later! #())
          (run-later! #(run! node-registry/execute-update! updates)))})))
