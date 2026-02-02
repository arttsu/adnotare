(ns adnotare.events
  (:require [cljfx.api :as fx]
            [adnotare.subs :as subs])
  (:import [java.util UUID]))

(defn- add-annotation [context kind start end text]
  (let [id (UUID/randomUUID)
        annotation {:start start
                    :end end
                    :kind kind
                    :text text
                    :note ""}]
    (-> context
        (update-in [:annotations] assoc id annotation)
        (assoc :selected-annotation-id id))))

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

(defn- annotations->xmlish [annotations kinds]
  (let [items (->> annotations
                   (map (fn [[id a]] (assoc a :id id)))
                   (sort-by :start))]
    (apply str
           (for [{:keys [text kind note]} items
                 :let [kind-text (get-in kinds [kind :text] "")]]
             (str "<annotation>\n"
                  "  <quote>\n" text "\n  </quote>\n"
                  "  <type>" kind-text "</type>\n"
                  "  <note>\n" note "\n  </note>\n"
                  "</annotation>\n\n")))))

(defmulti event-handler :event/type)

(defmethod event-handler :adnotare/rich-area-selection-changed [{:keys [fx/context start end selected-text]}]
  {:context (fx/swap-context context assoc :rich-area-selection {:start start
                                                                 :end end
                                                                 :selected-text selected-text})})

(defmethod event-handler :adnotare/add-annotation [{:keys [fx/context adnotare/kind]}]
  (let [{:keys [start end selected-text]} (subs/rich-area-selection context)]
    (if (.isEmpty selected-text)
      {}
      {:context (fx/swap-context context add-annotation kind start end selected-text)
       :dispatch-later {:ms 10
                        :event {:event/type :adnotare/post-add-annotation}}})))

(defmethod event-handler :adnotare/post-add-annotation [_]
  {:ui {:ops [{:op :clear-selection :node :editor}
              {:op :focus :node :additional-note}]}})

(defmethod event-handler :adnotare/select-annotation [{:keys [fx/context adnotare/id]}]
  {:context (fx/swap-context context assoc :selected-annotation-id id)
   :dispatch-later {:ms 10
                    :event {:event/type :adnotare/post-select-annotation}}})

(defmethod event-handler :adnotare/post-select-annotation [{:keys [fx/context]}]
  (let [{:keys [start end]} (subs/selected-annotation context)]
    {:ui {:ops [{:op :reveal-range :node :editor :start start :end end}
                {:op :focus :node :additional-note}]}}))

(defmethod event-handler :adnotare/consume-mouse-event [{:keys [fx/event]}]
  (.consume event)
  {})

(defmethod event-handler :adnotare/update-annotation-note [{:keys [fx/context adnotare/id fx/event]}]
  {:context (fx/swap-context context assoc-in [:annotations id :note] event)})

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

(defmethod event-handler :adnotare/copy-annotations [{:keys [fx/context]}]
  (let [annotations (subs/annotations context)
        kinds (subs/annotation-kinds context)
        toast-id (UUID/randomUUID)]
    (if (empty? annotations)
      {:context (fx/swap-context context update-in [:toasts] assoc toast-id {:text "No annotations to copy"
                                                                             :type "warning"
                                                                             :created-at (System/currentTimeMillis)})
       :dispatch-later {:ms 1500
                        :event {:event/type :adnotare/clear-toast
                                :adnotare/id toast-id}}}
      (let [s (annotations->xmlish annotations kinds)]
        {:copy-to-clipboard {:text s}
         :context (fx/swap-context context update-in [:toasts] assoc toast-id {:text "Copied annotations to clipboard"
                                                                               :type "success"
                                                                               :created-at (System/currentTimeMillis)})
         :dispatch-later {:ms 1500
                          :event {:event/type :adnotare/clear-toast
                                  :adnotare/id toast-id}}}))))

(defmethod event-handler :adnotare/clear-toast [{:keys [fx/context adnotare/id]}]
  {:context (fx/swap-context context update-in [:toasts] dissoc id)})
