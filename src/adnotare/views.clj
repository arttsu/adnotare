(ns adnotare.views
  (:require [adnotare.subs :as subs]
            [adnotare.rich :refer [annotated-area]]
            [clojure.java.io :as io]))

(defn resource-url ^String [path]
  (some-> (io/resource path) str))

(defn text [{:keys [fx/context]}]
  {:fx/type annotated-area
   :adnotare/model (subs/annotated-area-model context)})

(defn root [_]
  {:fx/type :stage
   :showing true
   :title "Adnotare"
   :width 800
   :height 600
   :scene {:fx/type :scene
           :stylesheets [(resource-url "app.css")]
           :root {:fx/type :v-box
                  :padding 10
                  :spacing 10
                  :children [{:fx/type text}]}}})
