(ns adnotare.core.event.annotate
  (:require
   [adnotare.core.derive.annotate :as derive.annotate]
   [adnotare.core.event :as event]
   [adnotare.core.schema :as S]
   [adnotare.core.state.document :as state.document]
   [adnotare.core.state.palettes :as state.palettes]
   [adnotare.core.state.ui :as state.ui]
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [malli.core :as m])
  (:import
   (java.util UUID)))

(defn on-select-annotation [state id]
  (event/result (state.ui.annotate/select-annotation state id)
                {:dispatch {:event/type :annotate/reveal-document-selection}
                 :dispatch-later {:ms 50
                                  :event {:event/type :annotate/focus-note}}}))
(m/=> on-select-annotation [:=> [:cat S/State :uuid] S/EventResult])
(defmethod event/handle :annotate/select-annotation [state {:keys [id]}]
  (on-select-annotation state id))

(defn on-reveal-document-selection [state]
  (let [selection (some-> (derive.annotate/selected-annotation state) :annotation/selection)]
    (event/result state
                  (if selection
                    {:ui {:updates [{:node-key :annotate/doc
                                     :op :reveal-range
                                     :selection {:start (:selection/start selection)
                                                 :end (:selection/end selection)
                                                 :text (:selection/text selection)}}]}}
                    {}))))
(m/=> on-reveal-document-selection [:=> [:cat S/State] S/EventResult])
(defmethod event/handle :annotate/reveal-document-selection [state _event]
  (on-reveal-document-selection state))

(defn on-add-annotation [state prompt-id]
  (event/result state
                {:get-selection {:node-key :annotate/doc
                                 :on-selection {:event/type :annotate/add-annotation-on-selection
                                                :prompt-id prompt-id}}}))
(m/=> on-add-annotation [:=> [:cat S/State :uuid] S/EventResult])
(defmethod event/handle :annotate/add-annotation [state {:keys [prompt-id]}]
  (on-add-annotation state prompt-id))

(defn on-delete-annotation [state id fx-event]
  (let [selected-id (state.ui.annotate/selected-annotation-id state)
        state (state.document/delete-annotation state id)
        state (if (= selected-id id)
                (state.ui.annotate/clear-annotation-selection state)
                state)]
    (event/result state
                  (if fx-event
                    {:consume-event fx-event}
                    {}))))
(m/=> on-delete-annotation [:=> [:cat S/State :uuid [:maybe :any]] S/EventResult])
(defmethod event/handle :annotate/delete-annotation [state {:keys [id] :as event-map}]
  (on-delete-annotation state id (:fx/event event-map)))

(defn on-add-annotation-on-selection [state prompt-id selection]
  (if (or (nil? selection) (empty? (:text selection)))
    (event/result state {:toast (state.ui/->toast "Please select some text first" :warning)})
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
        (event/result state
                      {:dispatch {:event/type :annotate/clear-document-selection}
                       :dispatch-later {:ms 50
                                        :event {:event/type :annotate/focus-note}}}))
      (event/result state {:toast (state.ui/->toast "Select a palette first" :warning)}))))
(m/=> on-add-annotation-on-selection
      [:=> [:cat S/State :uuid [:maybe S/UISelection]] S/EventResult])
(defmethod event/handle :annotate/add-annotation-on-selection [state {:keys [prompt-id selection]}]
  (on-add-annotation-on-selection state prompt-id selection))

(defn on-clear-document-selection [state]
  (event/result state
                {:ui {:updates [{:node-key :annotate/doc :op :clear-selection}]}}))
(m/=> on-clear-document-selection [:=> [:cat S/State] S/EventResult])
(defmethod event/handle :annotate/clear-document-selection [state _event]
  (on-clear-document-selection state))

(defn on-focus-note [state]
  (event/result state
                {:ui {:updates [{:node-key :annotate/selected-annotation-note :op :focus}]}}))
(m/=> on-focus-note [:=> [:cat S/State] S/EventResult])
(defmethod event/handle :annotate/focus-note [state _event]
  (on-focus-note state))

(defn on-paste-doc [state]
  (event/result state
                {:get-clipboard {:on-clipboard {:event/type :annotate/paste-doc-on-clipboard}}}))
(m/=> on-paste-doc [:=> [:cat S/State] S/EventResult])
(defmethod event/handle :annotate/paste-doc [state _event]
  (on-paste-doc state))

(defn on-paste-doc-on-clipboard [state text]
  (if (or (nil? text) (empty? text))
    (event/result state {:toast (state.ui/->toast "Clipboard is empty" :warning)})
    (let [replace-doc-event {:event/type :annotate/replace-doc :text text}
          any-annotations? (seq (derive.annotate/annotations state))]
      (if any-annotations?
        (event/result state
                      {:confirm {:title "Replace document text?"
                                 :header "Replace document text from clipboard?"
                                 :content "This will remove all existing annotations. Continue?"
                                 :yes-event replace-doc-event}})
        (event/result state {:dispatch replace-doc-event})))))
(m/=> on-paste-doc-on-clipboard [:=> [:cat S/State [:maybe :string]] S/EventResult])
(defmethod event/handle :annotate/paste-doc-on-clipboard [state {:keys [text]}]
  (on-paste-doc-on-clipboard state text))

(defn on-replace-doc [state text]
  (event/result (-> state
                    (state.document/replace-text text)
                    (state.ui.annotate/clear-annotation-selection))))
(m/=> on-replace-doc [:=> [:cat S/State :string] S/EventResult])
(defmethod event/handle :annotate/replace-doc [state {:keys [text]}]
  (on-replace-doc state text))

(defn on-update-selected-annotation-note [state text]
  (let [selected-id (state.ui.annotate/selected-annotation-id state)]
    (event/result (if selected-id
                    (state.document/update-annotation-note state selected-id text)
                    state))))
(m/=> on-update-selected-annotation-note [:=> [:cat S/State :string] S/EventResult])
(defmethod event/handle :annotate/update-selected-annotation-note [state {:keys [text]}]
  (on-update-selected-annotation-note state text))

(defn on-switch-palette [state palette-id]
  (let [state (-> state
                  (state.ui.annotate/set-active-palette palette-id)
                  (state.palettes/mark-last-used palette-id))]
    (event/result state {:persist-palettes {:palettes (:state/palettes state)}})))
(m/=> on-switch-palette [:=> [:cat S/State :uuid] S/EventResult])
(defmethod event/handle :annotate/switch-palette [state {:keys [palette-id]}]
  (on-switch-palette state palette-id))

(defn on-copy-annotations [state]
  (if (seq (derive.annotate/annotations state))
    (event/result state
                  {:copy-to-clipboard {:text (derive.annotate/annotations-str state)}
                   :toast (state.ui/->toast "Copied annotations to clipboard" :success)})
    (event/result state {:toast (state.ui/->toast "Add some annotations first" :warning)})))
(m/=> on-copy-annotations [:=> [:cat S/State] S/EventResult])
(defmethod event/handle :annotate/copy-annotations [state _event]
  (on-copy-annotations state))
