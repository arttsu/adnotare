(ns adnotare.app.subs
  (:require
   [adnotare.app.context :refer [Context]]
   [adnotare.core.features.ui :as ui]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.palettes :refer [Palettes]]
   [adnotare.core.model.toast :refer [Toast]]
   [adnotare.core.util.schema :refer [IDSeq]]
   [cljfx.api :as fx]
   [malli.core :as m]))

(defn initialized? [context]
  (fx/sub-val context ::app/initialized?))

(defn route [context]
  (fx/sub-val context ::app/route))

(defn app-state [context]
  (fx/sub-val context (fn [app] app)))

(defn toasts [context]
  (fx/sub-val context ui/toasts))
(m/=> toasts [:=> [:cat Context] (IDSeq Toast)])

(defn palettes [context]
  (fx/sub-val context ::app/palettes))
(m/=> palettes [:=> [:cat Context] Palettes])
