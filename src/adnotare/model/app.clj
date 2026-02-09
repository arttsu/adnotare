(ns adnotare.model.app
  (:require
   [adnotare.model.schema :as S]
   [malli.core :as m]))

(defn toasts [app]
  (->> (mapv (fn [[id toast]] (assoc toast :id id)) (get-in app [:toasts :by-id]))
       (sort-by :created-at-ms)))
(m/=> toasts [:-> S/App [:sequential S/Toast]])

(defn add-toast [app id toast]
  (assoc-in app [:toasts :by-id id] toast))
(m/=> add-toast [:-> S/App :uuid S/NormalizedToast S/App])

(defn clear-toast [app id]
  (update-in app [:toasts :by-id] dissoc id))
(m/=> clear-toast [:-> S/App :uuid S/App])
