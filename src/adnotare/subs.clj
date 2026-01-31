(ns adnotare.subs
  (:require [cljfx.api :as fx]))

(defn text [context]
  (fx/sub-val context :text))

(defn annotation-kinds [context]
  (fx/sub-val context :annotation-kinds))

(defn annotations [context]
  (fx/sub-val context :annotations))

(defn selected-annotation-id [context]
  (fx/sub-val context :selected-annotation-id))

(defn selected-annotation [context]
  (get (annotations context) (selected-annotation-id context)))

(defn annotated-area-model [context]
  (let [text (text context)
        kinds (annotation-kinds context)
        annotations (annotations context)
        selected (selected-annotation-id context)
        spans (map (fn [[id {:keys [start end kind]}]]
                     {:start start
                      :end end
                      :color (get-in kinds [kind :color])
                      :selected (= id selected)})
                   annotations)]
    {:text text
     :spans spans}))

(defn rich-area-selection [context]
  (fx/sub-val context :rich-area-selection))
