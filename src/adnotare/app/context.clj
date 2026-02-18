(ns adnotare.app.context
  (:require
   [adnotare.core.model.app :as app]))

(def Context
  [:map
   [:cljfx.context/m app/App]])
