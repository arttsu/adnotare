(ns adnotare.app.annotator.events
  (:require
   [adnotare.app.annotator.subs :as subs]
   [adnotare.app.interface :refer [handle-event]]
   [adnotare.core.features.annotate :as annotate]
   [adnotare.core.model.selection :as selection]
   [adnotare.core.model.toast :as toast :refer [->Toast]]
   [cljfx.api :as fx]))

(defmethod handle-event :annotator/add-annotation-from-selection [{:keys [prompt-id]}]
  {:code-area/get-selection {:code-area-key :annotator/document-code-area
                             :on-selection {:event/type :annotator/add-annotation
                                            :prompt-id prompt-id}}})

(defmethod handle-event :annotator/add-annotation [{:keys [fx/context prompt-id selection]}]
  (if (or (nil? selection) (empty? (:text selection)))
    {:toast {:toast (->Toast ::toast/warning "Select some text first")}}
    (let [selection' {::selection/start (:start selection)
                      ::selection/end (:end selection)
                      ::selection/quote (:text selection)}]
      {:context (fx/swap-context context annotate/add-annotation prompt-id selection')
       :update-nodes {:updates [{:operation :code-area/clear-selection :node-key :annotator/document-code-area}]}
       :dispatch-later {:delay-ms 50 :event {:event/type :annotator/focus-selected-annotation-note-text-area}}})))

(defmethod handle-event :annotator/select-annotation [{:keys [fx/context id]}]
  {:context (fx/swap-context context annotate/select-annotation id)
   :dispatch {:event/type :annotator/reveal-selected-annotation-in-document}
   :dispatch-later {:delay-ms 50 :event {:event/type :annotator/focus-selected-annotation-note-text-area :select-all true}}})

(defmethod handle-event :annotator/focus-selected-annotation-note-text-area [{:keys [select-all]}]
  (let [updates (cond-> [{:operation :focus :node-key :annotator/selected-annotation-note-text-area}]
                  select-all (conj {:operation :text-area/select-content :node-key :annotator/selected-annotation-note-text-area}))]
    {:update-nodes {:updates updates}}))

(defmethod handle-event :annotator/reveal-selected-annotation-in-document [{:keys [fx/context]}]
  (let [range (subs/selected-annotation-range context)]
    {:update-nodes {:updates [{:operation :code-area/reveal-range :node-key :annotator/document-code-area :range range}]}}))

(defmethod handle-event :annotator/delete-annotation [{:keys [fx/context id]}]
  {:context (fx/swap-context context annotate/delete-annotation id)})

(defmethod handle-event :annotator/update-selected-annotation-note [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context annotate/put-selected-annotation-note event)})

(defmethod handle-event :annotator/paste-document-from-clipboard [_]
  {:clipboard/get {:on-content {:event/type :annotator/update-document-with-clipboard-content}}})

(defmethod handle-event :annotator/update-document-with-clipboard-content [{:keys [fx/context content]}]
  (if (or (nil? content) (empty? content))
    {:toast {:toast (->Toast ::toast/warning "Clipboard is empty")}}
    (let [update-document {:event/type :annotator/update-document :text content}]
      (if (empty? (subs/annotations context))
        {:dispatch update-document}
        {:confirm {:title "Replace document text?"
                   :header "Replace document text from clipboard"
                   :content "This will remove all existing annotations. Continue?"
                   :on-yes update-document}}))))

(defmethod handle-event :annotator/update-document [{:keys [fx/context text]}]
  {:context (fx/swap-context context annotate/put-document-text text)
   :toast {:toast (->Toast ::toast/success "Pasted document text from clipboard")}})

(defmethod handle-event :annotator/copy-annotations-as-llm-prompt [{:keys [fx/context]}]
  (let [annotations (subs/annotations context)]
    (if (seq annotations)
      {:clipboard/put {:content (subs/annotations-as-llm-prompt context)}
       :toast {:toast (->Toast ::toast/success (str "Copied " (count annotations) " annotations to clipboard"))}}
      {:toast {:toast (->Toast ::toast/warning "No annotations to copy")}})))

(defmethod handle-event :annotator/copy-annotations-and-document-as-llm-prompt [{:keys [fx/context]}]
  (let [annotations (subs/annotations context)]
    (if (seq annotations)
      {:clipboard/put {:content (subs/annotations-and-document-as-llm-prompt context)}
       :toast {:toast (->Toast ::toast/success (str "Copied document and " (count annotations) " annotations to clipboard"))}}
      {:toast {:toast (->Toast ::toast/warning "No annotations to copy")}})))

(defmethod handle-event :annotator/switch-palette
  [{:keys [fx/context fx/event]}]
  (let [id (-> event .getSource .getValue :id)]
    {:context (fx/swap-context context annotate/switch-palette id)
     :dispatch-later {:delay-ms 10, :event {:event/type :ui/persist-palettes}}}))

(defmethod handle-event :annotator/switch-palette-next [{:keys [fx/context]}]
  {:context (fx/swap-context context annotate/switch-palette-next)
   :dispatch-later {:delay-ms 10, :event {:event/type :ui/persist-palettes}}})

(defmethod handle-event :annotator/switch-palette-prev [{:keys [fx/context]}]
  {:context (fx/swap-context context annotate/switch-palette-prev)
   :dispatch-later {:delay-ms 10, :event {:event/type :ui/persist-palettes}}})
