(ns adnotare.app.annotate.events
  (:require [adnotare.app.annotate.subs :as subs]
            [adnotare.fx.handler :refer [handle-event]]
            [adnotare.model.session :as session]
            [adnotare.model.toast :refer [->toast]]
            [cljfx.api :as fx]))

(defmethod handle-event :annotate/select-annotation [{:keys [fx/context id]}]
  {:context (fx/swap-context context update-in [:state/session] session/select-annotation id)
   :dispatch {:event/type :annotate/select-annotation-update-ui}})

(defmethod handle-event :annotate/select-annotation-update-ui [{:keys [fx/context]}]
  (let [selection (subs/selected-annotation-selection context)]
    {:ui {:updates [{:node-key :annotate/doc :op :reveal-range :selection selection}
                    {:node-key :annotate/selected-annotation-note :op :focus}]}}))

(defmethod handle-event :annotate/add-annotation [{:keys [prompt-id]}]
  {:get-selection {:node-key :annotate/doc
                   :on-selection {:event/type :annotate/add-annotation-on-selection
                                  :prompt-id prompt-id}}})

(defmethod handle-event :annotate/delete-annotation [{:keys [fx/context id fx/event]}]
  {:context (fx/swap-context context update-in [:state/session] session/delete-annotation id)
   :consume-event event})

;; FIX: Note area doesn't get focus after adding the first annotation. Probably because it's disabled at that point.
(defmethod handle-event :annotate/add-annotation-on-selection [{:keys [fx/context prompt-id selection]}]
  (if (.isEmpty (:text selection))
    {:toast (->toast "Please select some text first" :warning)}
    {:context (fx/swap-context context update-in [:state/session] session/add-annotation prompt-id selection)
     :dispatch {:event/type :annotate/add-annotation-update-ui}}))

;; FIX: The bug above is still present with this approach as well :(
(defmethod handle-event :annotate/add-annotation-update-ui [_]
  {:ui {:updates [{:node-key :annotate/doc :op :clear-selection}
                  {:node-key :annotate/selected-annotation-note :op :focus}]}})

(defmethod handle-event :annotate/paste-doc [_]
  {:get-clipboard {:on-clipboard {:event/type :annotate/paste-doc-on-clipboard}}})

(defmethod handle-event :annotate/paste-doc-on-clipboard [{:keys [fx/context text]}]
  (if (.isEmpty text)
    {:toast (->toast "Clipboard is empty" :warning)}
    (let [replace-doc-event {:event/type :annotate/replace-doc :text text}]
      (if (subs/any-annotations? context)
        {:confirm {:title "Replace document text?"
                   :header "Replace document text from clipboard?"
                   :content "This will remove all existing annotations. Continue?"
                   :yes-event replace-doc-event}}
        {:dispatch replace-doc-event}))))

(defmethod handle-event :annotate/replace-doc [{:keys [fx/context text]}]
  {:context (fx/swap-context context update-in [:state/session] session/replace-doc text)})

(defmethod handle-event :annotate/update-selected-annotation-note [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context update-in [:state/session] session/update-selected-annotation-note event)})

(defmethod handle-event :annotate/copy-annotations [{:keys [fx/context]}]
  (if (subs/any-annotations? context)
    {:copy-to-clipboard {:text (subs/annotations-for-llm context)}
     :toast (->toast "Copied annotations to clipboard" :success)}
    {:toast (->toast "Add some annotations first" :warning)}))
