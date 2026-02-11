(ns adnotare.app.subs
  (:require
   [adnotare.core.state.ui :as ui]
   [cljfx.api :as fx]))

(defn toasts [context]
  (fx/sub-val context ui/toasts))

(defn initialized? [context]
  (fx/sub-val context ui/initialized?))

(defn route [context]
  (fx/sub-val context ui/route))
