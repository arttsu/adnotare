(ns adnotare.model.toast
  (:require [malli.core :as m]
            [adnotare.model.schema :as S]))

(defn ->toast
  ([text type]
   (->toast text type 1500))
  ([text type duration-ms]
   {:text text
    :type type
    :duration-ms duration-ms
    :created-at-ms (System/currentTimeMillis)}))
(m/=> ->toast [:function
               [:-> S/Label S/ToastType S/Toast]
               [:-> S/Label S/ToastType S/Millis S/Toast]])
