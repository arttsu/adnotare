(ns adnotare.views
  (:require [cljfx.api :as fx]
            [adnotare.events :as events]
            [adnotare.subs :as subs]
            [adnotare.rich :refer [code-area]])
  (:import (org.fxmisc.richtext CodeArea)
           (java.net URL)))

(defn resource-url ^String [path]
  (some-> (clojure.java.io/resource path) str))

(defn text [{:keys [fx/context]}]
  {:fx/type code-area
   :desc {:fx/type fx/ext-instance-factory
          :create #(doto (CodeArea.)
                     (.setWrapText true))}
   :props {:adnotare/text (subs/text context)
           :adnotare/style-spans (subs/style-spans context)
           :adnotare/read-only? true}})

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
