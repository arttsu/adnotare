(ns adnotare.test.factory
  (:require
   [adnotare.util.uuid :as uuid]))

(defn- list->by-id [id-key items]
  (into {} (map (fn [item] [(id-key item) (dissoc item id-key)]) items)))

(defn ->palette [{:palette/keys [id label prompts] :or {id (uuid/random)}}]
  {:palette/id id
   :palette/label label
   :palette/prompts {:by-id (list->by-id :prompt/id prompts)
                     :order (mapv :prompt/id prompts)}})

(defn ->prompt [{:prompt/keys [id text color] :or {id (uuid/random)
                                                   color 1}}]
  {:prompt/id id :prompt/text text :prompt/color color})

(defn ->palettes [{:keys [palettes last-used-ms] :or {last-used-ms {}}}]
  {:by-id (into {} (map (fn [palette]
                                   [(:palette/id palette)
                                    (dissoc palette :palette/id)]))
                         palettes)
   :last-used-ms last-used-ms})
