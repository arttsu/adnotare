(ns adnotare.core.state.palettes
  (:require
   [clojure.string :as string]))

(defn palettes [state]
  (:state/palettes state))

(defn put-palettes [state palettes]
  (assoc state :state/palettes palettes))

(defn by-id [state]
  (get-in state [:state/palettes :palettes/by-id]))

(defn palette-by-id [state palette-id]
  (get-in state [:state/palettes :palettes/by-id palette-id]))

(defn palette-id-seq [state]
  (->> (by-id state)
       (sort-by (comp string/lower-case :palette/label val))
       (map key)))

(defn mark-last-used
  ([state palette-id]
   (mark-last-used state palette-id (System/currentTimeMillis)))
  ([state palette-id now-ms]
   (if (nil? palette-id)
     state
     (assoc-in state [:state/palettes :palettes/last-used-ms palette-id] now-ms))))

(defn most-recent-id [state]
  (let [last-used (get-in state [:state/palettes :palettes/last-used-ms])]
    (when (seq last-used)
      (first (apply max-key val last-used)))))

(defn first-palette-id [state]
  (first (palette-id-seq state)))
