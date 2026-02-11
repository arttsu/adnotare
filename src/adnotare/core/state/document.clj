(ns adnotare.core.state.document
  (:require
   [adnotare.core.schema :as S]
   [malli.core :as m]))

(defn document [state]
  (:state/document state))
(m/=> document [:=> [:cat S/State] S/Document])

(defn text [state]
  (get-in state [:state/document :document/text]))
(m/=> text [:=> [:cat S/State] :string])

(defn annotations [state]
  (get-in state [:state/document :document/annotations]))
(m/=> annotations [:=> [:cat S/State] S/NormalizedAnnotations])

(defn annotation-by-id [state annotation-id]
  (get-in state [:state/document :document/annotations :by-id annotation-id]))
(m/=> annotation-by-id [:=> [:cat S/State :uuid] [:maybe S/Annotation]])

(defn annotation-ids [state]
  (keys (get-in state [:state/document :document/annotations :by-id])))
(m/=> annotation-ids [:=> [:cat S/State] [:sequential :uuid]])

(defn replace-text [state text]
  (-> state
      (assoc-in [:state/document :document/text] text)
      (assoc-in [:state/document :document/annotations] {:by-id {}})))
(m/=> replace-text [:=> [:cat S/State :string] S/State])

(defn add-annotation [state annotation-id prompt-ref selection]
  (assoc-in state
            [:state/document :document/annotations :by-id annotation-id]
            {:annotation/prompt-ref prompt-ref
             :annotation/selection selection
             :annotation/note ""}))
(m/=> add-annotation [:=> [:cat S/State :uuid S/PromptRef S/Selection] S/State])

(defn update-annotation-note [state annotation-id text]
  (assoc-in state
            [:state/document :document/annotations :by-id annotation-id :annotation/note]
            text))
(m/=> update-annotation-note [:=> [:cat S/State :uuid :string] S/State])

(defn delete-annotation [state annotation-id]
  (update-in state [:state/document :document/annotations :by-id] dissoc annotation-id))
(m/=> delete-annotation [:=> [:cat S/State :uuid] S/State])
