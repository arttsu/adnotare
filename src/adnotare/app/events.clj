(ns adnotare.app.events
  (:require
   [adnotare.core.event :as core.event]
   [adnotare.fx.handler :refer [handle-event]]
   [cljfx.api :as fx])
  (:import
   (javafx.application Platform)))

(defn- apply-core [context event]
  (let [{:keys [state] :as result} (core.event/handle (fx/sub-val context identity) event)]
    (merge {:context (fx/reset-context context state)}
           (dissoc result :state))))

(defmethod handle-event :app/quit [_]
  (Platform/exit)
  (System/exit 0))

(defmethod handle-event :app/add-toast [{:keys [fx/context id toast]}]
  (apply-core context {:event/type :app/add-toast
                       :id id
                       :toast toast}))

(defmethod handle-event :app/clear-toast [{:keys [fx/context id]}]
  (apply-core context {:event/type :app/clear-toast
                       :id id}))

(defmethod handle-event :app/start [{:keys [fx/context]}]
  (apply-core context {:event/type :app/start}))

(defmethod handle-event :app/on-palettes-loaded [{:keys [fx/context status palettes reason]}]
  (apply-core context {:event/type :app/on-palettes-loaded
                       :status status
                       :palettes palettes
                       :reason reason}))

(defmethod handle-event :app/navigate [{:keys [fx/context route]}]
  (apply-core context {:event/type :app/navigate
                       :route route}))
