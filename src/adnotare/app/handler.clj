(ns adnotare.app.handler
  (:require
   [adnotare.app.annotate.events]
   [adnotare.app.events]
   [adnotare.app.manage-prompts.events]
   [adnotare.app.node-registry :as node-registry]
   [adnotare.core.persist.palettes :as persist.palettes]
   [adnotare.core.state :as state]
   [adnotare.fx.handler :refer [handle-event]] ;; PR: I think it's better to move it to adnotare.app. In 'interfaces' ns maybe?
   [adnotare.util.uuid :as uuid]
   [cljfx.api :as fx])
  (:import
   (javafx.animation PauseTransition)
   (javafx.application Platform)
   (javafx.event EventHandler)
   (javafx.scene.control Alert Alert$AlertType ButtonType)
   (javafx.scene.input Clipboard ClipboardContent)
   (javafx.util Duration)))

(def *state
  (atom (fx/create-context state/initial)))

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
        :dispatch-later
        (fn [{:keys [ms event]} dispatch]
          (doto (PauseTransition. (Duration/millis ms))
            (.setOnFinished (reify EventHandler
                              (handle [_ _]
                                (dispatch event))))
            (.play)))
        :consume-event
        (fn [event _dispatch]
          (.consume event))
        :ui
        (fn [{:keys [updates]} _dispatch]
          (run-later! #()) ;; PR: Probably this is no longer needed
          (run-later! #(run! node-registry/execute-update! updates)))
        :confirm
        ;; PR: I think we should move these fn handlers to their own ns.
        (fn [{:keys [title header content yes-event]} dispatch]
          (let [alert (doto (Alert. Alert$AlertType/CONFIRMATION)
                        (.setTitle title)
                        (.setHeaderText header)
                        (.setContentText content))
                result (.orElse (.showAndWait alert) ButtonType/CANCEL)]
            (when (= result ButtonType/OK)
              (dispatch yes-event))))
        :toast
        (fn [toast dispatch]
          (let [id (uuid/random)]
            (doto (PauseTransition. (Duration/millis (:toast/duration-ms toast)))
              (.setOnFinished (reify EventHandler
                                (handle [_ _]
                                  (dispatch {:event/type :app/clear-toast
                                             :id id}))))
              (.play))
            (dispatch {:event/type :app/add-toast
                       :id id
                       :toast toast})))
        ;; PR: Rename -> read-rich-text-area-selection
        :get-selection
        (fn [{:keys [node-key on-selection]} dispatch]
          (when-let [selection (node-registry/get-selection node-key)]
            (dispatch (assoc on-selection :selection selection))))
        ;; PR: Rename -> read-from-clipboard / write-to-clipboard
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
        :load-palettes
        (fn [{:keys [on-load]} dispatch]
          (let [result (persist.palettes/read-palettes)]
            (dispatch (merge on-load result))))
        :persist-palettes
        (fn [{:keys [palettes]} _dispatch]
          (persist.palettes/write-palettes! palettes))})))
