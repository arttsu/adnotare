(ns adnotare.app.annotate.events
  (:require
   [adnotare.app.annotate.subs :as subs]
   [adnotare.core.state.document :as state.document]
   [adnotare.core.state.palettes :as state.palettes]
   [adnotare.core.state.ui :as ui]
   [adnotare.core.state.ui.annotate :as ui.annotate]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx]))

(defmethod handle-event :annotate/select-annotation [{:keys [fx/context id]}]
  {:context (fx/swap-context context ui.annotate/select-annotation id)
   :dispatch {:event/type :annotate/reveal-document-selection}
   :dispatch-later {:ms 50
                    :event {:event/type :annotate/focus-note}}})

(defmethod handle-event :annotate/reveal-document-selection [{:keys [fx/context]}]
  (let [selection (subs/selected-annotation-selection context)]
    {:ui {:updates [{:node-key :annotate/doc :op :reveal-range :selection selection}]}}))

(defmethod handle-event :annotate/add-annotation [{:keys [prompt-id]}]
  {:get-selection {:node-key :annotate/doc
                   :on-selection {:event/type :annotate/add-annotation-on-selection
                                  :prompt-id prompt-id}}})

(defmethod handle-event :annotate/delete-annotation [{:keys [fx/context id fx/event]}]
  {:context (fx/swap-context context state.document/delete-annotation id)
   :consume-event event})

(defmethod handle-event :annotate/add-annotation-on-selection [{:keys [fx/context prompt-id selection]}]
  (if (.isEmpty (:text selection))
    {:toast (ui/->toast "Please select some text first" :warning)}
    {:context (fx/swap-context
               context
               (fn [state]
                 (state.document/add-annotation
                  state
                  {:prompt-ref/palette-id (ui.annotate/active-palette-id state)
                   :prompt-ref/prompt-id prompt-id}
                  {:selection/start (:start selection)
                   :selection/end (:end selection)
                   :selection/text (:text selection)})))
     :dispatch {:event/type :annotate/clear-document-selection}
     :dispatch-later {:ms 50
                      :event {:event/type :annotate/focus-note}}}))

(defmethod handle-event :annotate/clear-document-selection [_]
  {:ui {:updates [{:node-key :annotate/doc :op :clear-selection}]}})

(defmethod handle-event :annotate/focus-note [_]
  {:ui {:updates [{:node-key :annotate/selected-annotation-note :op :focus}]}})

(defmethod handle-event :annotate/paste-doc [_]
  {:get-clipboard {:on-clipboard {:event/type :annotate/paste-doc-on-clipboard}}})

(defmethod handle-event :annotate/paste-doc-on-clipboard [{:keys [fx/context text]}]
  (if (or (nil? text) (.isEmpty text))
    {:toast (ui/->toast "Clipboard is empty" :warning)}
    (let [replace-doc-event {:event/type :annotate/replace-doc :text text}]
      (if (subs/any-annotations? context)
        {:confirm {:title "Replace document text?"
                   :header "Replace document text from clipboard?"
                   :content "This will remove all existing annotations. Continue?"
                   :yes-event replace-doc-event}}
        {:dispatch replace-doc-event}))))

(defmethod handle-event :annotate/replace-doc [{:keys [fx/context text]}]
  {:context (fx/swap-context context state.document/replace-text text)})

(defmethod handle-event :annotate/update-selected-annotation-note [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context state.document/update-selected-annotation-note event)})

(defmethod handle-event :annotate/switch-palette [{:keys [fx/context fx/event]}]
  (let [palette-id (-> event .getSource .getValue :option/id)]
    (when palette-id
      (let [new-state (-> (fx/sub-val context identity)
                          (ui.annotate/set-active-palette palette-id)
                          (state.palettes/mark-last-used palette-id))]
        {:context (fx/swap-context context (constantly new-state))
         :persist-palettes {:palettes (:state/palettes new-state)}}))))

(defmethod handle-event :annotate/copy-annotations [{:keys [fx/context]}]
  (if (subs/any-annotations? context)
    {:copy-to-clipboard {:text (subs/annotations-str context)}
     :toast (ui/->toast "Copied annotations to clipboard" :success)}
    {:toast (ui/->toast "Add some annotations first" :warning)}))
