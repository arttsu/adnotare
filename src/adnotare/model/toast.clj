(ns adnotare.model.toast
  (:require
   [adnotare.model.schema :as S]
   [malli.core :as m]))

(defn ->toast
  ([text type]
   (->toast text type 1500))
  ([text type duration-ms]
   {:text text
    :type type
    :duration-ms duration-ms
    :created-at-ms (System/currentTimeMillis)}))
(m/=> ->toast [:function
               [:-> S/Label S/ToastType S/NormalizedToast]
               [:-> S/Label S/ToastType S/Millis S/NormalizedToast]])
