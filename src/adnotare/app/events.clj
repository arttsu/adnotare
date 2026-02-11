(ns adnotare.app.events
  (:require
   [adnotare.core.state :as state]
   [adnotare.core.state.ui :as ui]
   [adnotare.core.state.ui.manage-prompts :as ui.manage-prompts]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx])
  (:import
   (javafx.application Platform)))

(defmethod handle-event :app/quit [_]
  (Platform/exit)
  (System/exit 0))

(defmethod handle-event :app/add-toast [{:keys [fx/context id toast]}]
  {:context (fx/swap-context context ui/add-toast id toast)})

(defmethod handle-event :app/clear-toast [{:keys [fx/context id]}]
  {:context (fx/swap-context context ui/clear-toast id)})

(defmethod handle-event :app/start [_]
  {:load-palettes {:on-load {:event/type :app/on-palettes-loaded}}})

(defmethod handle-event :app/on-palettes-loaded [{:keys [fx/context status palettes reason]}]
  (let [toast (if (= :ok status)
                (ui/->toast "Initialized successfully" :success)
                (ui/->toast (str "Loading persisted palettes failed: " (or reason "unknown error")) :error))
        new-state (state/with-palettes state/initial palettes)]
    {:context (fx/reset-context context new-state)
     :toast toast}))

(defmethod handle-event :app/navigate [{:keys [fx/context route]}]
  {:context (fx/swap-context
             context
             (fn [s]
               (let [s (ui/set-route s route)]
                 (if (= route :manage-prompts)
                   (ui.manage-prompts/sync-with-active-palette s)
                   s))))})
