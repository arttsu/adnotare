(ns adnotare.core.model.prompt
  (:require
   [adnotare.core.util.schema :refer [Color Label]]))

(def Prompt
  [:map
   [::text Label]
   [::color Color]])
