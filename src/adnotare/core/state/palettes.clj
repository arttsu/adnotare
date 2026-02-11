(ns adnotare.core.state.palettes
  (:require
   [adnotare.core.schema :as S]
   [clojure.string :as string]
   [malli.core :as m]))

(defn palettes [state]
  (:state/palettes state))
(m/=> palettes [:=> [:cat S/State] S/Palettes])

(defn put-palettes [state palettes]
  (assoc state :state/palettes palettes))
(m/=> put-palettes [:=> [:cat S/State S/Palettes] S/State])

(defn by-id [state]
  (get-in state [:state/palettes :palettes/by-id]))
(m/=> by-id [:=> [:cat S/State] [:map-of :uuid S/Palette]])

(defn palette-by-id [state palette-id]
  (get-in state [:state/palettes :palettes/by-id palette-id]))
(m/=> palette-by-id [:=> [:cat S/State :uuid] [:maybe S/Palette]])

(defn palette-id-seq [state]
  (->> (by-id state)
       (sort-by (comp string/lower-case :palette/label val))
       (map key)))
(m/=> palette-id-seq [:=> [:cat S/State] [:sequential :uuid]])

(defn mark-last-used
  ([state palette-id]
   (mark-last-used state palette-id (System/currentTimeMillis)))
  ([state palette-id now-ms]
   (assoc-in state [:state/palettes :palettes/last-used-ms palette-id] now-ms)))
(m/=> mark-last-used
      [:function
       [:=> [:cat S/State :uuid] S/State]
       [:=> [:cat S/State :uuid S/Millis] S/State]])

(defn most-recent-id [state]
  (let [last-used (get-in state [:state/palettes :palettes/last-used-ms])]
    (when (seq last-used)
      (first (apply max-key val last-used)))))
(m/=> most-recent-id [:=> [:cat S/State] [:maybe :uuid]])

(defn first-palette-id [state]
  (first (palette-id-seq state)))
(m/=> first-palette-id [:=> [:cat S/State] [:maybe :uuid]])
