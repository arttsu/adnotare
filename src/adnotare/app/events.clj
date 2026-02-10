(ns adnotare.app.events
  (:require
   [adnotare.fx.handler :refer [handle-event]]
   [adnotare.model.app :as app]
   [adnotare.model.toast :refer [->toast]]
   [cljfx.api :as fx])
  (:import
   (javafx.application Platform)))

(defmethod handle-event :app/quit [_]
  (Platform/exit)
  (System/exit 0))

(defmethod handle-event :app/add-toast [{:keys [fx/context id toast]}]
  {:context (fx/swap-context context update-in [:state/app] app/add-toast id toast)})

(defmethod handle-event :app/clear-toast [{:keys [fx/context id]}]
  {:context (fx/swap-context context update-in [:state/app] app/clear-toast id)})

(defmethod handle-event :app/start [_]
  {:init-session {:on-init {:event/type :app/on-init}}})

(defmethod handle-event :app/on-init [{:keys [fx/context status state reason]}]
  (let [toast (if (= :ok status)
                (->toast "Initialized successfully" :success)
                (->toast (str "Loading persisted session failed: " (or reason "unknown error")) :error))]
    {:context (fx/reset-context context state)
     :toast toast}))
