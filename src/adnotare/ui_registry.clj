(ns adnotare.ui-registry
  (:require
   [cljfx.api :as fx])
  (:import (javafx.scene Node)))

(defonce *registry (atom {}))

(defn node ^Node [key]
  (get @*registry key))

(defn registered [key desc]
  {:fx/type fx/ext-on-instance-lifecycle
   :desc desc
   :on-created (fn [^Node node]
                 (swap! *registry assoc key node))
   :on-deleted (fn [_node]
                 (swap! *registry dissoc key))})
