(ns adnotare.app.state
  (:require
   [adnotare.app.annotator.events]
   [adnotare.app.events]
   [adnotare.app.interface :refer [handle-event]]
   [adnotare.app.node-registry :as node-registry]
   [adnotare.app.prompt-manager.events]
   [adnotare.core.model.app :as core.model.app]
   [adnotare.core.util.uuid :as uuid]
   [adnotare.fx.extensions.code-area :as code-area]
   [adnotare.persistence.palettes :refer [write-palettes!]]
   [cljfx.api :as fx])
  (:import
   (javafx.animation PauseTransition)
   (javafx.application Platform)
   (javafx.event EventHandler)
   (javafx.scene.control Alert Alert$AlertType ButtonType)
   (javafx.scene.control TextInputControl)
   (javafx.scene.input Clipboard ClipboardContent)
   (javafx.util Duration)))

(defn- run-later! [f]
  (if (Platform/isFxApplicationThread)
    (f)
    (Platform/runLater f)))

(defn- execute-node-update! [{:keys [node-key operation] :as update}]
  (when-let [node (node-registry/node node-key)]
    (case operation
      :focus (.requestFocus node)
      :text-area/select-content (.selectAll node)
      :text-input/select-content (.selectAll ^TextInputControl node)
      :code-area/clear-selection (code-area/clear-selection! node)
      :code-area/reveal-range (code-area/reveal-range! node (:range update)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Effect handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(defn- dispatch-later [payload dispatch]
  (let [schedules (cond
                    (nil? payload) []
                    (sequential? payload) payload
                    :else [payload])]
    (doseq [{:keys [delay-ms event]} schedules]
      (doto (PauseTransition. (Duration/millis delay-ms))
        (.setOnFinished (reify EventHandler
                          (handle [_ _]
                            (dispatch event))))
        (.play)))))

(defn- confirm [{:keys [title header content on-yes]} dispatch]
  (let [alert (doto (Alert. Alert$AlertType/CONFIRMATION)
                (.setTitle title)
                (.setHeaderText header)
                (.setContentText content))
        result (.orElse (.showAndWait alert) ButtonType/CANCEL)]
    (when (= result ButtonType/OK)
      (dispatch on-yes))))

(defn- consume-event! [event _dispatch]
  (.consume event))

(defn- update-nodes! [{:keys [updates]} _dispatch]
  (run-later! #(run! execute-node-update! updates)))

(defn- code-area-get-selection [{:keys [code-area-key on-selection]} dispatch]
  (let [code-area (node-registry/node code-area-key)]
    (dispatch (assoc on-selection :selection (code-area/get-selection code-area)))))

(defn- get-clipboard-content [{:keys [on-content]} dispatch]
  (let [clipboard (Clipboard/getSystemClipboard)
        content (when (.hasString clipboard) (.getString clipboard))]
    (dispatch (assoc on-content :content content))))

(defn- put-clipboard-content! [{:keys [content]} _dispatch]
  (let [clipboard (Clipboard/getSystemClipboard)
        clipboard-content (doto (ClipboardContent.)
                            (.putString content))]
    (.setContent clipboard clipboard-content)))

(defn- toast! [{:keys [duration-ms toast] :or {duration-ms 1500}} dispatch]
  (let [id (uuid/random)]
    (doto (PauseTransition. (Duration/millis duration-ms))
      (.setOnFinished (reify EventHandler
                        (handle [_ _]
                          (dispatch {:event/type :app/clear-toast :id id}))))
      (.play))
    (dispatch {:event/type :app/add-toast :id id :toast toast})))

(defn- persist-palettes! [{:keys [palettes]} _dispatch]
  (write-palettes! palettes))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Event handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;

(def *state
  (atom (fx/create-context core.model.app/base)))

(def event-handler
  (-> handle-event
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context (fx/make-reset-effect *state)
        :dispatch fx/dispatch-effect
        :dispatch-later dispatch-later
        :confirm confirm
        :toast toast!
        :consume-event consume-event!
        :update-nodes update-nodes!
        :code-area/get-selection code-area-get-selection
        :clipboard/get get-clipboard-content
        :clipboard/put put-clipboard-content!
        :persist-palettes persist-palettes!})))
