(ns adnotare.core.state.ui
  (:require
   [adnotare.core.schema :as S]
   [malli.core :as m]))

(defn ui [state]
  (:state/ui state))
(m/=> ui [:=> [:cat S/State] S/UI])

(defn route [state]
  (get-in state [:state/ui :ui/route]))
(m/=> route [:=> [:cat S/State] S/Route])

(defn set-route [state route]
  (assoc-in state [:state/ui :ui/route] route))
(m/=> set-route [:=> [:cat S/State S/Route] S/State])

(defn initialized? [state]
  (get-in state [:state/ui :ui/initialized?]))
(m/=> initialized? [:=> [:cat S/State] :boolean])

(defn set-initialized [state initialized?]
  (assoc-in state [:state/ui :ui/initialized?] initialized?))
(m/=> set-initialized [:=> [:cat S/State :boolean] S/State])

(defn toasts [state]
  (->> (get-in state [:state/ui :ui/toasts :by-id])
       (mapv (fn [[id toast]] (assoc toast :toast/id id)))
       (sort-by :toast/created-at-ms)))
(m/=> toasts [:=> [:cat S/State] [:sequential S/DerivedToast]])

(defn add-toast [state id toast]
  (assoc-in state [:state/ui :ui/toasts :by-id id] toast))
(m/=> add-toast [:=> [:cat S/State :uuid S/NormalizedToast] S/State])

(defn clear-toast [state id]
  (update-in state [:state/ui :ui/toasts :by-id] dissoc id))
(m/=> clear-toast [:=> [:cat S/State :uuid] S/State])

(defn ->toast
  ([text type]
   (->toast text type 1500))
  ([text type duration-ms]
   {:toast/text text
    :toast/type type
    :toast/duration-ms duration-ms
    :toast/created-at-ms (System/currentTimeMillis)}))
(m/=> ->toast
      [:function
       [:=> [:cat S/Label S/ToastType] S/NormalizedToast]
       [:=> [:cat S/Label S/ToastType S/Millis] S/NormalizedToast]])
