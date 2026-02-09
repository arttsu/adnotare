(ns adnotare.app.views
  (:require [adnotare.app.annotate.views :as annotate]
            [adnotare.app.subs :as subs]
            [adnotare.util.resources :as resources]))

(defn- toast-banner [{:keys [text type]}]
  {:fx/type :h-box
   :style-class ["toast" (name type)]
   :padding 10
   :alignment :center-left
   :children [{:fx/type :label
               :text text
               :max-width 360}]})

(defn- toast-list [{:keys [fx/context]}]
  (let [toasts (subs/toasts context)]
    {:fx/type :v-box
     :pick-on-bounds false
     :alignment :top-right
     :spacing 10
     :padding 14
     :fill-width false
     :visible (any? toasts)
     :children (map toast-banner toasts)}))

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
     [{:fx/type annotate/root}
      {:fx/type toast-list
       :stack-pane/alignment :top-right}]}}})
