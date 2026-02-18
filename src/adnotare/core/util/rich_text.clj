(ns adnotare.core.util.rich-text
  (:require
   [adnotare.core.util.schema :refer [Identifier]]
   [malli.util :as mu]))

(def Range
  [:map
   [:start :int]
   [:end :int]])

(def Span
  (mu/merge
   Range
   [:map
    [:style-class [:sequential Identifier]]]))

(def RichText
  [:map
   [:text :string]
   [:spans [:sequential Span]]])
