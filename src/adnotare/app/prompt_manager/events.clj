(ns adnotare.app.prompt-manager.events
  (:require
   [adnotare.app.interface :refer [handle-event]]
   [adnotare.app.prompt-manager.subs :as subs]
   [adnotare.app.subs :as app-subs]
   [adnotare.core.features.manage-prompts :as manage-prompts]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.model.toast :as toast :refer [->Toast]]
   [cljfx.api :as fx]))

(defn- schedule-persist [persist-token]
  {:dispatch-later
   {:delay-ms 400
    :event {:event/type :prompt-manager/persist-palettes-debounced
            :persist-token persist-token}}})

(defn- blocking-invalid-draft-effect []
  {:toast {:toast (->Toast ::toast/warning "Fix validation errors before leaving")}})

(defmethod handle-event :prompt-manager/select-palette
  [{:keys [fx/context id]}]
  (if (subs/invalid-draft? context)
    (blocking-invalid-draft-effect)
    {:context (fx/swap-context context manage-prompts/select-palette id)
     :dispatch-later {:delay-ms 50
                      :event {:event/type :prompt-manager/focus-palette-label}}}))

(defmethod handle-event :prompt-manager/select-prompt
  [{:keys [fx/context id]}]
  (if (subs/invalid-draft? context)
    (blocking-invalid-draft-effect)
    {:context (fx/swap-context context manage-prompts/select-prompt id)
     :dispatch-later {:delay-ms 50
                      :event {:event/type :prompt-manager/focus-prompt-label}}}))

(defmethod handle-event :prompt-manager/focus-palette-label
  [_]
  {:update-nodes {:updates [{:operation :focus :node-key :prompt-manager/palette-label}
                            {:operation :text-input/select-content :node-key :prompt-manager/palette-label}]}})

(defmethod handle-event :prompt-manager/focus-prompt-label
  [_]
  {:update-nodes {:updates [{:operation :focus :node-key :prompt-manager/prompt-label}
                            {:operation :text-input/select-content :node-key :prompt-manager/prompt-label}]}})

(defmethod handle-event :prompt-manager/add-palette
  [{:keys [fx/context]}]
  (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
    {:context (fx/swap-context context manage-prompts/add-palette*)
     :dispatch-later [{:delay-ms 50
                       :event {:event/type :prompt-manager/focus-palette-label}}
                      (:dispatch-later (schedule-persist persist-token))]}))

(defmethod handle-event :prompt-manager/add-prompt
  [{:keys [fx/context]}]
  (if-let [palette-id (subs/selected-palette-id context)]
    (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
      {:context (fx/swap-context context manage-prompts/add-prompt* palette-id)
       :dispatch-later [{:delay-ms 50
                         :event {:event/type :prompt-manager/focus-prompt-label}}
                        (:dispatch-later (schedule-persist persist-token))]})
    {}))

(defmethod handle-event :prompt-manager/update-palette-label
  [{:keys [fx/context fx/event]}]
  (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
    (merge
     {:context (fx/swap-context context manage-prompts/update-palette-label* event)}
     (schedule-persist persist-token))))

(defmethod handle-event :prompt-manager/update-prompt-label
  [{:keys [fx/context fx/event]}]
  (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
    (merge
     {:context (fx/swap-context context manage-prompts/update-prompt-label* event)}
     (schedule-persist persist-token))))

(defmethod handle-event :prompt-manager/update-prompt-instructions
  [{:keys [fx/context fx/event]}]
  (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
    (merge
     {:context (fx/swap-context context manage-prompts/update-prompt-instructions* event)}
     (schedule-persist persist-token))))

(defmethod handle-event :prompt-manager/update-prompt-color
  [{:keys [fx/context color]}]
  (if-let [palette-id (subs/selected-palette-id context)]
    (if-let [[prompt-id _] (subs/selected-prompt context)]
      (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
        (merge
         {:context (fx/swap-context context manage-prompts/update-prompt-color* palette-id prompt-id color)}
         (schedule-persist persist-token)))
      {})
    {}))

(defmethod handle-event :prompt-manager/request-delete-palette
  [{:keys [fx/context palette-id]}]
  (let [app-state (app-subs/app-state context)]
    (if-not (manage-prompts/can-delete-palette? app-state)
      {:toast {:toast (->Toast ::toast/warning "Cannot delete the last palette")}}
      (let [selected-palette-id (subs/selected-palette-id context)
            target-palette-id (or palette-id selected-palette-id)
            palette (get-in app-state [::app/palettes :by-id target-palette-id])
            label (::palette/label palette)
            count (manage-prompts/annotation-count-for-palette app-state target-palette-id)]
        (if target-palette-id
          {:confirm {:title "Delete palette?"
                     :header "Delete selected palette"
                     :content (str "Delete palette \"" label
                                   "\"? This will remove " count " annotations. Continue?")
                     :on-yes {:event/type :prompt-manager/confirm-delete-palette
                              :palette-id target-palette-id}}}
          {})))))

(defmethod handle-event :prompt-manager/confirm-delete-palette
  [{:keys [fx/context palette-id]}]
  (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
    (merge
     {:context (fx/swap-context context manage-prompts/delete-palette* palette-id)}
     (schedule-persist persist-token))))

(defmethod handle-event :prompt-manager/request-delete-prompt
  [{:keys [fx/context prompt-id]}]
  (let [app-state (app-subs/app-state context)]
    (if-let [palette-id (subs/selected-palette-id context)]
      (let [selected-prompt-id (subs/selected-prompt-id context)
            target-prompt-id (or prompt-id selected-prompt-id)
            prompt (get-in app-state [::app/palettes :by-id palette-id ::palette/prompts :by-id target-prompt-id])]
        (if (nil? target-prompt-id)
          {}
          (if-not (manage-prompts/can-delete-prompt? app-state palette-id)
            {:toast {:toast (->Toast ::toast/warning "Cannot delete the last prompt")}}
            (let [count (manage-prompts/annotation-count-for-prompt app-state palette-id target-prompt-id)]
              (if (pos? count)
                {:confirm {:title "Delete prompt?"
                           :header "Delete selected prompt"
                           :content (str "Delete prompt \"" (::prompt/label prompt)
                                         "\"? This will remove " count " annotations. Continue?")
                           :on-yes {:event/type :prompt-manager/confirm-delete-prompt
                                    :palette-id palette-id
                                    :prompt-id target-prompt-id}}}
                {:dispatch {:event/type :prompt-manager/confirm-delete-prompt
                            :palette-id palette-id
                            :prompt-id target-prompt-id}})))))
      {})))

(defmethod handle-event :prompt-manager/confirm-delete-prompt
  [{:keys [fx/context palette-id prompt-id]}]
  (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
    (merge
     {:context (fx/swap-context context manage-prompts/delete-prompt* palette-id prompt-id)}
     (schedule-persist persist-token))))

(defmethod handle-event :prompt-manager/move-prompt
  [{:keys [fx/context prompt-id direction]}]
  (if-let [palette-id (subs/selected-palette-id context)]
    (let [target-prompt-id (or prompt-id (subs/selected-prompt-id context))]
      (if target-prompt-id
        (let [persist-token (inc (fx/sub-val context ::app/persist-token))]
          (merge
           {:context (fx/swap-context context manage-prompts/move-prompt* palette-id target-prompt-id direction)}
           (schedule-persist persist-token)))
        {}))
    {}))

(defmethod handle-event :prompt-manager/persist-palettes-debounced
  [{:keys [fx/context persist-token]}]
  (let [current-token (fx/sub-val context ::app/persist-token)
        app-state (app-subs/app-state context)]
    (cond
      (not= persist-token current-token)
      {}

      (manage-prompts/invalid-draft? app-state)
      {:toast {:toast (->Toast ::toast/warning "Not saved: fix validation errors")}}

      :else
      {:persist-palettes {:palettes (app-subs/palettes context)}
       :toast {:toast (->Toast ::toast/success "Saved palettes")}})))

(defmethod handle-event :prompt-manager/navigate-back
  [{:keys [fx/context]}]
  (if (subs/invalid-draft? context)
    (blocking-invalid-draft-effect)
    {:dispatch {:event/type :ui/navigate :route ::app/annotator}}))
