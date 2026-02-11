(ns adnotare.core.event.app
  (:require
   [adnotare.core.event :as event]
   [adnotare.core.schema :as S]
   [adnotare.core.state :as state]
   [adnotare.core.state.ui :as state.ui]
   [adnotare.core.state.ui.manage-prompts :as state.ui.manage-prompts]
   [malli.core :as m]))

(defn on-add-toast [state id toast]
  (event/result (state.ui/add-toast state id toast)))
(m/=> on-add-toast [:=> [:cat S/State :uuid S/NormalizedToast] S/EventResult])
(defmethod event/handle :app/add-toast [state {:keys [id toast]}]
  (on-add-toast state id toast))

(defn on-clear-toast [state id]
  (event/result (state.ui/clear-toast state id)))
(m/=> on-clear-toast [:=> [:cat S/State :uuid] S/EventResult])
(defmethod event/handle :app/clear-toast [state {:keys [id]}]
  (on-clear-toast state id))

(defn on-start [state]
  (event/result state
                {:load-palettes {:on-load {:event/type :app/on-palettes-loaded}}}))
(m/=> on-start [:=> [:cat S/State] S/EventResult])
(defmethod event/handle :app/start [state _event]
  (on-start state))

(defn on-palettes-loaded [state status palettes reason]
  (let [toast (if (= :ok status)
                (state.ui/->toast "Initialized successfully" :success)
                (state.ui/->toast (str "Loading persisted palettes failed: " (or reason "unknown error"))
                                  :error))
        new-state (state/with-palettes state (or palettes state/default-palettes))]
    (event/result new-state {:toast toast})))
(m/=> on-palettes-loaded
      [:=> [:cat S/State [:enum :ok :error] [:maybe S/Palettes] [:maybe :string]] S/EventResult])
(defmethod event/handle :app/on-palettes-loaded [state {:keys [status palettes reason]}]
  (on-palettes-loaded state status palettes reason))

(defn on-navigate [state route]
  (let [state (state.ui/set-route state route)
        state (if (= route :manage-prompts)
                (state.ui.manage-prompts/sync-with-active-palette state)
                state)]
    (event/result state)))
(m/=> on-navigate [:=> [:cat S/State S/Route] S/EventResult])
(defmethod event/handle :app/navigate [state {:keys [route]}]
  (on-navigate state route))
