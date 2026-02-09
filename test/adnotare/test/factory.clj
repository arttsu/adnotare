(ns adnotare.test.factory 
  (:require
   [adnotare.util.uuid :as uuid]))

(defn- list->by-id [items]
  (into {} (map (fn [item] [(:id item) item]) items)))

(defn ->palette [{:keys [id label prompts] :or {id (uuid/random)}}]
  {:id id
   :label label
   :prompts {:by-id (list->by-id prompts)
             :order (map :id prompts)}})

(defn ->prompt [{:keys [id text color] :or {id (uuid/random)
                                            color 1}}]
  {:id id :text text :color color})

(defn ->palettes [{:keys [palettes last-used-ms] :or {last-used-ms {}}}]
  {:by-id (list->by-id palettes)
   :last-used-ms last-used-ms})
