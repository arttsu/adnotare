(ns adnotare.core.model.document
  (:require
   [adnotare.core.model.annotation :as annotation :refer [Annotation]]
   [adnotare.core.model.selection :as selection]
   [adnotare.core.util.schema :refer [IDSeq]]
   [malli.core :as m]))

(def Document
  [:and
   [:map
    [::text :string]
    [::annotations
     [:map
      [:by-id [:map-of :uuid Annotation]]]]]
   [:fn {:error/message "All annotation bounds must be <= text length"}
    (fn [{::keys [text annotations]}]
      (let [text-length (count text)]
        (every? (fn [[_id annotation]]
                  (let [{::selection/keys [start end]} (::annotation/selection annotation)]
                    (and (< start text-length) (<= end text-length))))
                (:by-id annotations))))]])

(def base
  {::text ""
   ::annotations
   {:by-id {}}})

(defn annotations [document]
  (sort-by (fn [[_id annotation]] (get-in annotation [::annotation/selection ::selection/start]))
           (get-in document [::annotations :by-id])))
(m/=> annotations [:=> [:cat Document] (IDSeq Annotation)])
