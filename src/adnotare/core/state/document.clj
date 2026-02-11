(ns adnotare.core.state.document
  (:require
   [adnotare.core.state.ui.annotate :as ui.annotate])
  (:import
   (java.util UUID)))

(defn document [state]
  (:state/document state))

(defn text [state]
  (get-in state [:state/document :document/text]))

(defn annotations [state]
  (get-in state [:state/document :document/annotations]))

(defn annotation-by-id [state annotation-id]
  (get-in state [:state/document :document/annotations :by-id annotation-id]))

(defn annotation-ids [state]
  (seq (get-in state [:state/document :document/annotations :order])))

(defn replace-text [state text]
  (-> state
      (assoc-in [:state/document :document/text] text)
      (assoc-in [:state/document :document/annotations] {:by-id {}
                                                         :order []})
      (ui.annotate/clear-annotation-selection)))

(defn add-annotation
  ([state prompt-ref selection]
   (add-annotation state prompt-ref selection UUID/randomUUID))
  ([state prompt-ref selection uuid-gen]
   (let [palette-id (:prompt-ref/palette-id prompt-ref)
         prompt-id (:prompt-ref/prompt-id prompt-ref)]
     (if (or (nil? palette-id) (nil? prompt-id))
       state
       (let [annotation-id (uuid-gen)
             annotation {:annotation/prompt-ref prompt-ref
                         :annotation/selection selection
                         :annotation/note ""}]
         (if (nil? annotation-id)
           state
           (-> state
               (assoc-in [:state/document :document/annotations :by-id annotation-id] annotation)
               (update-in [:state/document :document/annotations :order] conj annotation-id)
               (ui.annotate/select-annotation annotation-id))))))))

(defn update-selected-annotation-note [state text]
  (let [annotation-id (ui.annotate/selected-annotation-id state)]
    (if (nil? annotation-id)
      state
      (assoc-in state
                [:state/document :document/annotations :by-id annotation-id :annotation/note]
                text))))

(defn delete-annotation [state annotation-id]
  (if (nil? annotation-id)
    state
    (let [selected-id (ui.annotate/selected-annotation-id state)]
      (cond-> (-> state
                  (update-in [:state/document :document/annotations :by-id] dissoc annotation-id)
                  (update-in [:state/document :document/annotations :order]
                             (fn [order]
                               (vec (remove #(= % annotation-id) order)))))
        (= annotation-id selected-id) (ui.annotate/clear-annotation-selection)))))
