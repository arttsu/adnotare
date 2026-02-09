(ns adnotare.app.annotate.events
  (:require [adnotare.app.annotate.subs :as subs]
            [adnotare.fx.handler :refer [handle-event]]
            [adnotare.model.session :as session]
            [adnotare.model.toast :refer [->toast]]
            [cljfx.api :as fx]))

(defmethod handle-event :annotate/select-annotation [{:keys [fx/context id]}]
  {:context (fx/swap-context context session/select-annotation id)
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
  {:context (fx/swap-context context session/delete-annotation id)
   :consume-event event})

(defmethod handle-event :annotate/add-annotation-on-selection [{:keys [fx/context prompt-id selection]}]
  (let [palette-id (subs/active-palette-id context)
        prompt-ref {:palette-id palette-id :prompt-id prompt-id}]
    (if (.isEmpty (:text selection))
      ;; TODO: Reduce nesting.
      {:toast {:toast (->toast "Please select some text first" :warning)}}
      {:context (fx/swap-context context session/add-annotation prompt-ref selection)
       :ui {:updates [{:node-key :annotate/doc :op :clear-selection}
                      {:node-key :annotate/selected-annotation-note :op :focus}]}})))

(defmethod handle-event :annotate/paste-doc [_]
  {:get-clipboard {:on-clipboard {:event/type :annotate/paste-doc-on-clipboard}}})

(defmethod handle-event :annotate/paste-doc-on-clipboard [{:keys [fx/context text]}]
  (if (.isEmpty text)
    {:toast {:toast (->toast "Clipboard is empty" :warning)}}
    (let [replace-doc-event {:event/type :annotate/replace-doc :text text}]
      (if (subs/any-annotations? context)
        {:confirm {:title "Replace document text?"
                   :header "Replace document text from clipboard?"
                   :content "This will remove all existing annotations. Continue?"
                   :yes-event replace-doc-event}}
        {:dispatch replace-doc-event}))))

(defmethod handle-event :annotate/replace-doc [{:keys [fx/context text]}]
  {:context (fx/swap-context context session/replace-doc text)})

(defmethod handle-event :annotate/update-selected-annotation-note [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context session/update-selected-annotation-note event)})

(defmethod handle-event :annotate/copy-annotations [{:keys [fx/context]}]
  {:copy-to-clipboard {:text (subs/annotations-str context)}
   :toast {:toast (->toast "Copied annotations to clipboard" :success)}})
