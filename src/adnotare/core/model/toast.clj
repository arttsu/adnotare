(ns adnotare.core.model.toast
  (:require
   [adnotare.core.util.schema :refer [Label Millis]]
   [malli.core :as m]))

(def ToastType [:enum ::success ::warning ::error ::info])

(def Toast
  [:map
   [::type ToastType]
   [::text Label]
   [::created-at-ms Millis]])

(defn ->Toast
  ([type text]
   (->Toast type text (System/currentTimeMillis)))
  ([type text created-at-ms]
   {::type type ::text text ::created-at-ms created-at-ms}))
(m/=> ->Toast [:function
               [:=> [:cat ToastType Label] Toast]
               [:=> [:cat ToastType Label Millis] Toast]])

