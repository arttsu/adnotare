(ns adnotare.app.annotate.events
  (:require
   [adnotare.core.derive.annotate :as derive.annotate]
   [adnotare.core.state.document :as state.document]
   [adnotare.core.state.palettes :as state.palettes]
   [adnotare.core.state.ui :as state.ui]
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx])
  (:import
   (java.util UUID)))

(defmethod handle-event :annotate/select-annotation [{:keys [fx/context id]}]
  {:context (fx/swap-context context state.ui.annotate/select-annotation id)
   :dispatch {:event/type :annotate/reveal-document-selection}
   :dispatch-later {:ms 50
                    :event {:event/type :annotate/focus-note}}})

(defmethod handle-event :annotate/reveal-document-selection [{:keys [fx/context]}]
  (let [state (fx/sub-val context identity)
        selection (some-> (derive.annotate/selected-annotation state) :annotation/selection)]
    (merge
     {:context (fx/swap-context context identity)}
     (if selection
       {:ui {:updates [{:node-key :annotate/doc
                        :op :reveal-range
                        :selection {:start (:selection/start selection)
                                    :end (:selection/end selection)
                                    :text (:selection/text selection)}}]}}
       {}))))

(defmethod handle-event :annotate/add-annotation [{:keys [fx/context prompt-id]}]
  {:context (fx/swap-context context identity)
   :get-selection {:node-key :annotate/doc
                   :on-selection {:event/type :annotate/add-annotation-on-selection
                                  :prompt-id prompt-id}}})

(defmethod handle-event :annotate/delete-annotation [{:keys [fx/context id fx/event]}]
  (let [state (fx/sub-val context identity)
        selected-id (state.ui.annotate/selected-annotation-id state)
        state (state.document/delete-annotation state id)
        state (if (= selected-id id)
                (state.ui.annotate/clear-annotation-selection state)
                state)]
    (merge
     {:context (fx/swap-context context (constantly state))}
     (if event
       {:consume-event event}
       {}))))

(defmethod handle-event :annotate/add-annotation-on-selection [{:keys [fx/context prompt-id selection]}]
  (let [state (fx/sub-val context identity)]
    (if (or (nil? selection) (empty? (:text selection)))
      {:context (fx/swap-context context identity)
       :toast (state.ui/->toast "Please select some text first" :warning)}
      (if-let [palette-id (state.ui.annotate/active-palette-id state)]
        (let [annotation-id (UUID/randomUUID)
              state (state.document/add-annotation
                     state
                     annotation-id
                     {:prompt-ref/palette-id palette-id
                      :prompt-ref/prompt-id prompt-id}
                     {:selection/start (:start selection)
                      :selection/end (:end selection)
                      :selection/text (:text selection)})
              state (state.ui.annotate/select-annotation state annotation-id)]
          {:context (fx/swap-context context (constantly state))
           :dispatch {:event/type :annotate/clear-document-selection}
           :dispatch-later {:ms 50
                            :event {:event/type :annotate/focus-note}}})
        {:context (fx/swap-context context identity)
         :toast (state.ui/->toast "Select a palette first" :warning)}))))

(defmethod handle-event :annotate/clear-document-selection [{:keys [fx/context]}]
  {:context (fx/swap-context context identity)
   :ui {:updates [{:node-key :annotate/doc :op :clear-selection}]}})

(defmethod handle-event :annotate/focus-note [{:keys [fx/context]}]
  {:context (fx/swap-context context identity)
   :ui {:updates [{:node-key :annotate/selected-annotation-note :op :focus}]}})

(defmethod handle-event :annotate/paste-doc [{:keys [fx/context]}]
  {:context (fx/swap-context context identity)
   :get-clipboard {:on-clipboard {:event/type :annotate/paste-doc-on-clipboard}}})

(defmethod handle-event :annotate/paste-doc-on-clipboard [{:keys [fx/context text]}]
  (let [state (fx/sub-val context identity)]
    (if (or (nil? text) (empty? text))
      {:context (fx/swap-context context identity)
       :toast (state.ui/->toast "Clipboard is empty" :warning)}
      (let [replace-doc-event {:event/type :annotate/replace-doc :text text}
            any-annotations? (seq (derive.annotate/annotations state))]
        (if any-annotations?
          {:context (fx/swap-context context identity)
           :confirm {:title "Replace document text?"
                     :header "Replace document text from clipboard?"
                     :content "This will remove all existing annotations. Continue?"
                     :yes-event replace-doc-event}}
          {:context (fx/swap-context context identity)
           :dispatch replace-doc-event})))))

(defmethod handle-event :annotate/replace-doc [{:keys [fx/context text]}]
  {:context (fx/swap-context context
                             (fn [state]
                               (-> state
                                   (state.document/replace-text text)
                                   (state.ui.annotate/clear-annotation-selection))))})

(defmethod handle-event :annotate/update-selected-annotation-note [{:keys [fx/context fx/event]}]
  (let [state (fx/sub-val context identity)
        selected-id (state.ui.annotate/selected-annotation-id state)
        state (if selected-id
                (state.document/update-annotation-note state selected-id event)
                state)]
    {:context (fx/swap-context context (constantly state))}))

(defmethod handle-event :annotate/switch-palette [{:keys [fx/context fx/event]}]
  (let [palette-id (-> event .getSource .getValue :option/id)]
    (if palette-id
      (let [state (-> (fx/sub-val context identity)
                      (state.ui.annotate/set-active-palette palette-id)
                      (state.palettes/mark-last-used palette-id))]
        {:context (fx/swap-context context (constantly state))
         :persist-palettes {:palettes (:state/palettes state)}})
      {:context (fx/swap-context context identity)})))

(defmethod handle-event :annotate/copy-annotations [{:keys [fx/context]}]
  (let [state (fx/sub-val context identity)]
    (if (seq (derive.annotate/annotations state))
      {:context (fx/swap-context context identity)
       :copy-to-clipboard {:text (derive.annotate/annotations-str state)}
       :toast (state.ui/->toast "Copied annotations to clipboard" :success)}
      {:context (fx/swap-context context identity)
       :toast (state.ui/->toast "Add some annotations first" :warning)})))
