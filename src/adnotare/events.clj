(ns adnotare.events
  (:require [cljfx.api :as fx]
            [adnotare.subs :as subs])
  (:import [java.util UUID]))

(defn- delete-annotation [context id]
  (-> context
      (update-in [:annotations] dissoc id)
      (assoc :selected-annotation-id nil)))

(defn- swap-text [context text]
  (assoc context
         :text text
         :annotations {}
         :selected-annotation-id nil
         :rich-area-selection {:start 0 :end 0 :selected-text ""}))

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

(defmethod event-handler :adnotare/paste-text [{:keys [fx/context]}]
  (if (empty? (subs/annotations context))
    {:dispatch {:event/type :adnotare/paste-text-from-clipboard}}
    {:confirm {:title "Replace text?"
               :header "Replace text from clipboard?"
               :content "This will remove all existing annotations. Continue?"
               :yes-event {:event/type :adnotare/paste-text-from-clipboard}}}))

(defmethod event-handler :adnotare/paste-text-from-clipboard [_]
  {:paste-from-clipboard nil})

(defmethod event-handler :adnotare/swap-text [{:keys [fx/context text]}]
  {:context (fx/swap-context context swap-text text)})
