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

(defn- xml-escape ^String [s]
  (-> (str (or s ""))
      (.replace "&" "&amp;")
      (.replace "<" "&lt;")
      (.replace ">" "&gt;")
      (.replace "\"" "&quot;")
      (.replace "'" "&apos;")))

(defn- annotations->xmlish [annotations kinds]
  (let [items (->> annotations
                   (map (fn [[id a]] (assoc a :id id)))
                   (sort-by :start))]
    (apply str
           (for [{:keys [text kind note]} items
                 :let [kind-text (get-in kinds [kind :text] "")]]
             (str "<annotation>\n"
                  "  <quote>\n" (xml-escape text) "\n  </quote>\n"
                  "  <type>" (xml-escape kind-text) "</type>\n"
                  "  <note>\n" (xml-escape note) "\n  </note>\n"
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
       :dispatch {:event/type :adnotare/editor-clear-selection}})))

(defmethod event-handler :adnotare/select-annotation [{:keys [fx/context adnotare/id]}]
  {:context (fx/swap-context context assoc :selected-annotation-id id)})

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
      {:context (fx/swap-context context update-in [:toasts] assoc toast-id {:text "No annotations to copy"})
       :dispatch-later {:ms 1500
                        :event {:event/type :adnotare/clear-toast
                                :adnotare/id toast-id}}}
      (let [s (annotations->xmlish annotations kinds)]
        {:copy-to-clipboard {:text s}
         :context (fx/swap-context context update-in [:toasts] assoc toast-id {:text "Copied annotations to clipboard"})
         :dispatch-later {:ms 1500
                          :event {:event/type :adnotare/clear-toast
                                  :adnotare/id toast-id}}}))))

(defmethod event-handler :adnotare/clear-toast [{:keys [fx/context adnotare/id]}]
  {:context (fx/swap-context context update-in [:toasts] dissoc id)})

(defn- issue-editor-command [context cmd]
  ;; nonce ensures the command map is different each time, even when
  ;; issuing the same command multiple times.
  (assoc context :editor-command (assoc cmd :nonce (UUID/randomUUID))))

(defmethod event-handler :adnotare/editor-clear-selection [{:keys [fx/context]}]
  {:context (fx/swap-context context issue-editor-command {:op :clear-selection})})
