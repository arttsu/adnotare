(ns adnotare.app.node-registry
  (:require [cljfx.api :as fx]
            [adnotare.fx.extensions.code-area :refer [clear-selection! reveal-range! pane->selection]])
  (:import (javafx.scene Node)
           (org.fxmisc.flowless VirtualizedScrollPane)))

(defonce *registry (atom {}))

(defn get-node ^Node [key]
  (get @*registry key))

(defn registered [key desc]
  {:fx/type fx/ext-on-instance-lifecycle
   :desc desc
   :on-created (fn [^Node node]
                 (swap! *registry assoc key node))
   :on-deleted (fn [_node]
                 (swap! *registry dissoc key))})

(defn execute-update! [{:keys [node-key op] :as update}]
  (if-let [target-node (get-node node-key)]
    ;; TODO: Use 'cond' and handle 'unsupported operation' case generically
    (case op
      :clear-selection
      (if (instance? VirtualizedScrollPane target-node)
        (clear-selection! target-node)
        (prn :error (str "Node " node-key " doesn't support :clear-selection operation")))
      :reveal-range
      (if (instance? VirtualizedScrollPane target-node)
        (reveal-range! target-node (:selection update))
        (prn :error (str "Node " node-key " doesn't support :reveal-range operation")))
      :focus
      (.requestFocus target-node))
    (prn :error (str "Node not found: " node-key))))

(defn get-selection [node-key]
  (when-let [pane (get-node node-key)]
    (when (instance? VirtualizedScrollPane pane)
      (pane->selection pane))))
