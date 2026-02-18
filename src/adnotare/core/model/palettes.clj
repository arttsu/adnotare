(ns adnotare.core.model.palettes
  (:require
   [adnotare.core.model.palette :refer [Palette]]
   [adnotare.core.util.schema :refer [Millis]]))

(def Palettes
  [:map
   [:by-id [:map-of :uuid Palette]]
   [:last-used-ms [:map-of :uuid Millis]]])

(def base
  {:by-id {}
   :last-used-ms {}})
