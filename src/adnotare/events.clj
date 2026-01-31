(ns adnotare.events
  (:require [cljfx.api :as fx]
            [adnotare.subs :as subs])
  (:import [java.util UUID]))

(defn delete-annotation [context id]
  (-> context
      (update-in [:annotations] dissoc id)
      (assoc :selected-annotation-id nil)))

(defmulti event-handler :event/type)

(defmethod event-handler :adnotare/rich-area-selection-changed [{:keys [fx/context start end selected-text]}]
  {:context (fx/swap-context context assoc :rich-area-selection {:start start
                                                                 :end end
                                                                 :selected-text selected-text})})

(defmethod event-handler :adnotare/add-annotation [{:keys [fx/context adnotare/kind]}]
  (let [{:keys [start end selected-text]} (subs/rich-area-selection context)]
    (if (.isEmpty selected-text)
      {}
      (let [id (UUID/randomUUID)]
        {:context (fx/swap-context context update-in [:annotations] assoc id {:start start :end end :kind kind :text selected-text})}))))

(defmethod event-handler :adnotare/select-annotation [{:keys [fx/context adnotare/id]}]
  {:context (fx/swap-context context assoc :selected-annotation-id id)})

(defmethod event-handler :adnotare/consume-mouse-event [{:keys [fx/event]}]
  (.consume event)
  {})

(defmethod event-handler :adnotare/delete-annotation [{:keys [fx/context adnotare/id]}]
  {:context (fx/swap-context context delete-annotation id)})
