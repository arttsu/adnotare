(ns adnotare.core.state.ui)

(defn ui [state]
  (:state/ui state))

(defn route [state]
  (get-in state [:state/ui :ui/route]))

(defn set-route [state route]
  (assoc-in state [:state/ui :ui/route] route))

(defn initialized? [state]
  (get-in state [:state/ui :ui/initialized?]))

(defn set-initialized [state initialized?]
  (assoc-in state [:state/ui :ui/initialized?] initialized?))

(defn toasts [state]
  (->> (get-in state [:state/ui :ui/toasts :by-id])
       (mapv (fn [[id toast]] (assoc toast :toast/id id)))
       (sort-by :toast/created-at-ms)))

(defn add-toast [state id toast]
  (if (nil? id)
    state
    (assoc-in state [:state/ui :ui/toasts :by-id id] toast)))

(defn clear-toast [state id]
  (if (nil? id)
    state
    (update-in state [:state/ui :ui/toasts :by-id] dissoc id)))

(defn ->toast
  ([text type]
   (->toast text type 1500))
  ([text type duration-ms]
   {:toast/text text
    :toast/type type
    :toast/duration-ms duration-ms
    :toast/created-at-ms (System/currentTimeMillis)}))
