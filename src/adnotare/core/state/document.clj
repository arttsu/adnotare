(ns adnotare.core.state.document)

(defn document [state]
  (:state/document state))

(defn text [state]
  (get-in state [:state/document :document/text]))

(defn annotations [state]
  (get-in state [:state/document :document/annotations]))

(defn annotation-by-id [state annotation-id]
  (get-in state [:state/document :document/annotations :by-id annotation-id]))

(defn annotation-ids [state]
  (keys (get-in state [:state/document :document/annotations :by-id])))

(defn replace-text [state text]
  (-> state
      (assoc-in [:state/document :document/text] text)
      (assoc-in [:state/document :document/annotations] {:by-id {}})))

(defn add-annotation [state annotation-id prompt-ref selection]
  (let [palette-id (:prompt-ref/palette-id prompt-ref)
        prompt-id (:prompt-ref/prompt-id prompt-ref)]
    (if (or (nil? annotation-id) (nil? palette-id) (nil? prompt-id))
      state
      (assoc-in state
                [:state/document :document/annotations :by-id annotation-id]
                {:annotation/prompt-ref prompt-ref
                 :annotation/selection selection
                 :annotation/note ""}))))

(defn update-annotation-note [state annotation-id text]
  (if (nil? annotation-id)
    state
    (assoc-in state
              [:state/document :document/annotations :by-id annotation-id :annotation/note]
              text)))

(defn delete-annotation [state annotation-id]
  (if (nil? annotation-id)
    state
    (update-in state [:state/document :document/annotations :by-id] dissoc annotation-id)))
