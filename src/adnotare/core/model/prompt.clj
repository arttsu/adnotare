(ns adnotare.core.model.prompt
  (:require
   [adnotare.core.util.schema :refer [Color Label]]
   [clojure.string :as string]
   [malli.core :as m]))

(def Prompt
  [:map
   [::label Label]
   [::instructions :string]
   [::color Color]])

(defn effective-text [{::keys [label instructions]}]
  (if (string/blank? instructions) label instructions))
(m/=> effective-text [:=> [:cat [:map [::label Label] [::instructions :string]]] :string])
