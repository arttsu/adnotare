(ns adnotare.core.event
  (:require
   [adnotare.core.derive.annotate :as derive.annotate]
   [adnotare.core.state :as state]
   [adnotare.core.state.document :as state.document]
   [adnotare.core.state.palettes :as state.palettes]
   [adnotare.core.state.ui :as state.ui]
   [adnotare.core.state.ui.annotate :as ui.annotate]
   [adnotare.core.state.ui.manage-prompts :as ui.manage-prompts])
  (:import
   (java.util UUID)))

(defn- result
  ([state]
   {:state state})
  ([state effect-map]
   (assoc effect-map :state state)))

(defmulti handle (fn [_state event] (:event/type event)))

(defmethod handle :default [state _event]
  (result state))

(defmethod handle :app/add-toast [state {:keys [id toast]}]
  (result (state.ui/add-toast state id toast)))

(defmethod handle :app/clear-toast [state {:keys [id]}]
  (result (state.ui/clear-toast state id)))

(defmethod handle :app/start [state _event]
  (result state
          {:load-palettes {:on-load {:event/type :app/on-palettes-loaded}}}))

(defmethod handle :app/on-palettes-loaded [state {:keys [status palettes reason]}]
  (let [toast (if (= :ok status)
                (state.ui/->toast "Initialized successfully" :success)
                (state.ui/->toast (str "Loading persisted palettes failed: " (or reason "unknown error"))
                                  :error))
        new-state (state/with-palettes state (or palettes state/default-palettes))]
    (result new-state {:toast toast})))

(defmethod handle :app/navigate [state {:keys [route]}]
  (let [state (state.ui/set-route state route)
        state (if (= route :manage-prompts)
                (ui.manage-prompts/sync-with-active-palette state)
                state)]
    (result state)))

(defmethod handle :annotate/select-annotation [state {:keys [id]}]
  (result (ui.annotate/select-annotation state id)
          {:dispatch {:event/type :annotate/reveal-document-selection}
           :dispatch-later {:ms 50
                            :event {:event/type :annotate/focus-note}}}))

(defmethod handle :annotate/reveal-document-selection [state _event]
  (let [selection (some-> (derive.annotate/selected-annotation state) :annotation/selection)]
    (result state
            (if selection
              {:ui {:updates [{:node-key :annotate/doc
                               :op :reveal-range
                               :selection {:start (:selection/start selection)
                                           :end (:selection/end selection)
                                           :text (:selection/text selection)}}]}}
              {}))))

(defmethod handle :annotate/add-annotation [state {:keys [prompt-id]}]
  (result state
          {:get-selection {:node-key :annotate/doc
                           :on-selection {:event/type :annotate/add-annotation-on-selection
                                          :prompt-id prompt-id}}}))

(defmethod handle :annotate/delete-annotation [state {:keys [id] :as event}]
  (let [selected-id (ui.annotate/selected-annotation-id state)
        state (state.document/delete-annotation state id)
        state (if (= selected-id id)
                (ui.annotate/clear-annotation-selection state)
                state)]
    (result state
            (if-let [fx-event (:fx/event event)]
              {:consume-event fx-event}
              {}))))

(defmethod handle :annotate/add-annotation-on-selection [state {:keys [prompt-id selection]}]
  (if (or (nil? selection) (empty? (:text selection)))
    (result state {:toast (state.ui/->toast "Please select some text first" :warning)})
    (let [annotation-id (UUID/randomUUID)
          state (state.document/add-annotation
                 state
                 annotation-id
                 {:prompt-ref/palette-id (ui.annotate/active-palette-id state)
                  :prompt-ref/prompt-id prompt-id}
                 {:selection/start (:start selection)
                  :selection/end (:end selection)
                  :selection/text (:text selection)})
          added? (some? (state.document/annotation-by-id state annotation-id))
          state (if added?
                  (ui.annotate/select-annotation state annotation-id)
                  state)]
      (if added?
        (result state
                {:dispatch {:event/type :annotate/clear-document-selection}
                 :dispatch-later {:ms 50
                                  :event {:event/type :annotate/focus-note}}})
        (result state {:toast (state.ui/->toast "Select a palette first" :warning)})))))

(defmethod handle :annotate/clear-document-selection [state _event]
  (result state
          {:ui {:updates [{:node-key :annotate/doc :op :clear-selection}]}}))

(defmethod handle :annotate/focus-note [state _event]
  (result state
          {:ui {:updates [{:node-key :annotate/selected-annotation-note :op :focus}]}}))

(defmethod handle :annotate/paste-doc [state _event]
  (result state
          {:get-clipboard {:on-clipboard {:event/type :annotate/paste-doc-on-clipboard}}}))

(defmethod handle :annotate/paste-doc-on-clipboard [state {:keys [text]}]
  (if (or (nil? text) (empty? text))
    (result state {:toast (state.ui/->toast "Clipboard is empty" :warning)})
    (let [replace-doc-event {:event/type :annotate/replace-doc :text text}
          any-annotations? (seq (derive.annotate/annotations state))]
      (if any-annotations?
        (result state
                {:confirm {:title "Replace document text?"
                           :header "Replace document text from clipboard?"
                           :content "This will remove all existing annotations. Continue?"
                           :yes-event replace-doc-event}})
        (result state {:dispatch replace-doc-event})))))

(defmethod handle :annotate/replace-doc [state {:keys [text]}]
  (result (-> state
              (state.document/replace-text text)
              (ui.annotate/clear-annotation-selection))))

(defmethod handle :annotate/update-selected-annotation-note [state {:keys [text]}]
  (let [selected-id (ui.annotate/selected-annotation-id state)]
    (result (state.document/update-annotation-note state selected-id text))))

(defmethod handle :annotate/switch-palette [state {:keys [palette-id]}]
  (if palette-id
    (let [state (-> state
                    (ui.annotate/set-active-palette palette-id)
                    (state.palettes/mark-last-used palette-id))]
      (result state {:persist-palettes {:palettes (:state/palettes state)}}))
    (result state)))

(defmethod handle :annotate/copy-annotations [state _event]
  (if (seq (derive.annotate/annotations state))
    (result state
            {:copy-to-clipboard {:text (derive.annotate/annotations-str state)}
             :toast (state.ui/->toast "Copied annotations to clipboard" :success)})
    (result state {:toast (state.ui/->toast "Add some annotations first" :warning)})))

(defmethod handle :manage-prompts/select-palette [state {:keys [palette-id]}]
  (if palette-id
    (result (ui.manage-prompts/select-palette state palette-id))
    (result state)))

(defmethod handle :manage-prompts/select-prompt [state {:keys [prompt-id]}]
  (if prompt-id
    (result (ui.manage-prompts/select-prompt state prompt-id))
    (result state)))
