(ns adnotare.app.events
  (:require
   [adnotare.core.state :as state]
   [adnotare.core.state.ui :as state.ui]
   [adnotare.core.state.ui.manage-prompts :as state.ui.manage-prompts]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx])
  (:import
   (javafx.application Platform)))

(defn- apply-state [context state effects]
  (merge {:context (fx/reset-context context state)} effects))

(defmethod handle-event :app/quit [_]
  (Platform/exit)
  (System/exit 0))

(defmethod handle-event :app/add-toast [{:keys [fx/context id toast]}]
  (let [state (-> (fx/sub-val context identity)
                  (state.ui/add-toast id toast))]
    (apply-state context state {})))

(defmethod handle-event :app/clear-toast [{:keys [fx/context id]}]
  (let [state (-> (fx/sub-val context identity)
                  (state.ui/clear-toast id))]
    (apply-state context state {})))

(defmethod handle-event :app/start [{:keys [fx/context]}]
  (apply-state context
               (fx/sub-val context identity)
               {:load-palettes {:on-load {:event/type :app/on-palettes-loaded}}}))

(defmethod handle-event :app/on-palettes-loaded [{:keys [fx/context status palettes reason]}]
  (let [current (fx/sub-val context identity)
        toast (if (= :ok status)
                (state.ui/->toast "Initialized successfully" :success)
                (state.ui/->toast (str "Loading persisted palettes failed: " (or reason "unknown error"))
                                  :error))
        state (state/with-palettes current (or palettes state/default-palettes))]
    (apply-state context state {:toast toast})))

(defmethod handle-event :app/navigate [{:keys [fx/context route]}]
  (let [current (fx/sub-val context identity)
        state (state.ui/set-route current route)
        state (if (= route :manage-prompts)
                (state.ui.manage-prompts/sync-with-active-palette state)
                state)]
    (apply-state context state {})))
