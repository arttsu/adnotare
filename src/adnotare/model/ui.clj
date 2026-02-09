(ns adnotare.model.ui
  (:require [adnotare.model.schema :as S]
            [malli.core :as m]))

(defn ->denorm-toast [id toast]
  (assoc toast :id id))

(defn toasts [state]
  (mapv (fn [[id toast]] (->denorm-toast id toast)) (get-in state [:ui :toasts :by-id])))
(m/=> toasts [:-> S/State [:sequential S/DenormToast]])

(defn add-toast [state id toast]
  (assoc-in state [:ui :toasts :by-id id] toast))
(m/=> add-toast [:-> S/State :uuid S/Toast S/State])

(defn clear-toast [state id]
  (update-in state [:ui :toasts :by-id] dissoc id))
(m/=> clear-toast [:-> S/State :uuid S/State])
