(ns adnotare.subs
  (:require [cljfx.api :as fx]))

(defn text [context]
  (fx/sub-val context :text))

(defn annotation-kinds [context]
  (fx/sub-val context :annotation-kinds))

(defn annotations [context]
  (fx/sub-val context :annotations))

(defn selected-annotation [context]
  (fx/sub-val context :selected-annotation))

(defn annotated-area-model [context]
  (let [text (text context)
        kinds (annotation-kinds context)
        annotations (annotations context)
        selected (selected-annotation context)
        spans (map (fn [{:keys [id start end kind]}]
                     {:start start
                      :end end
                      :color (get-in kinds [kind :color])
                      :selected (= id selected)})
                   annotations)]
    {:text text
     :spans spans}))
