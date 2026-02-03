(ns adnotare.app.views
  (:require [adnotare.util.resources :as resources]
            [adnotare.app.annotate.views :refer [annotate]]
            [adnotare.app.subs :as subs]))

(defn- ->toast [{:keys [text type]}]
  {:fx/type :h-box
   :style-class ["toast" (name type)]
   :padding 10
   :alignment :center-left
   :children [{:fx/type :label
               :text text
               :max-width 360}]})

(defn- toasts [{:keys [fx/context]}]
  (let [toasts (subs/sorted-toasts context)]
    {:fx/type :v-box
     :pick-on-bounds false
     :alignment :top-right
     :spacing 10
     :padding 14
     :fill-width false
     :visible (any? toasts)
     :children (map ->toast toasts)}))

(defn root [_]
  {:fx/type :stage
   :showing true
   :title "Adnotare"
   :width 1600
   :height 1200
   :on-close-request {:event/type :app/quit}
   :scene
   {:fx/type :scene
    :stylesheets [(resources/url "app.css")]
    :root
    {:fx/type :stack-pane
     :children
     [{:fx/type annotate}
      {:fx/type toasts
       :stack-pane/alignment :top-right}]}}})
