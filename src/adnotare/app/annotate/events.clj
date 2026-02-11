(ns adnotare.app.annotate.events
  (:require
   [adnotare.core.event :as core.event]
   [adnotare.core.event.annotate]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx]))

(defn- apply-core
  ([context event]
   (apply-core context event nil))
  ([context event env]
   (let [{:keys [state] :as result} (core.event/handle (fx/sub-val context identity)
                                                       (merge event env))]
     (merge {:context (fx/reset-context context state)}
            (dissoc result :state)))))

(defmethod handle-event :annotate/select-annotation [{:keys [fx/context id]}]
  (apply-core context {:event/type :annotate/select-annotation
                       :id id}))

(defmethod handle-event :annotate/reveal-document-selection [{:keys [fx/context]}]
  (apply-core context {:event/type :annotate/reveal-document-selection}))

(defmethod handle-event :annotate/add-annotation [{:keys [fx/context prompt-id]}]
  (apply-core context {:event/type :annotate/add-annotation
                       :prompt-id prompt-id}))

(defmethod handle-event :annotate/delete-annotation [{:keys [fx/context id fx/event]}]
  (apply-core context
              {:event/type :annotate/delete-annotation
               :id id}
              {:fx/event event}))

(defmethod handle-event :annotate/add-annotation-on-selection [{:keys [fx/context prompt-id selection]}]
  (apply-core context {:event/type :annotate/add-annotation-on-selection
                       :prompt-id prompt-id
                       :selection selection}))

(defmethod handle-event :annotate/clear-document-selection [{:keys [fx/context]}]
  (apply-core context {:event/type :annotate/clear-document-selection}))

(defmethod handle-event :annotate/focus-note [{:keys [fx/context]}]
  (apply-core context {:event/type :annotate/focus-note}))

(defmethod handle-event :annotate/paste-doc [{:keys [fx/context]}]
  (apply-core context {:event/type :annotate/paste-doc}))

(defmethod handle-event :annotate/paste-doc-on-clipboard [{:keys [fx/context text]}]
  (apply-core context {:event/type :annotate/paste-doc-on-clipboard
                       :text text}))

(defmethod handle-event :annotate/replace-doc [{:keys [fx/context text]}]
  (apply-core context {:event/type :annotate/replace-doc
                       :text text}))

(defmethod handle-event :annotate/update-selected-annotation-note [{:keys [fx/context fx/event]}]
  (apply-core context {:event/type :annotate/update-selected-annotation-note
                       :text event}))

(defmethod handle-event :annotate/switch-palette [{:keys [fx/context fx/event]}]
  (let [palette-id (-> event .getSource .getValue :option/id)]
    (apply-core context {:event/type :annotate/switch-palette
                         :palette-id palette-id})))

(defmethod handle-event :annotate/copy-annotations [{:keys [fx/context]}]
  (apply-core context {:event/type :annotate/copy-annotations}))
