(ns adnotare.core.state.palettes)

(defn palettes [state]
  (:state/palettes state))

(defn put-palettes [state palettes]
  (assoc state :state/palettes palettes))

(defn by-id [state]
  (get-in state [:state/palettes :palettes/by-id]))

(defn palette-by-id [state palette-id]
  (get-in state [:state/palettes :palettes/by-id palette-id]))

(defn order [state]
  (get-in state [:state/palettes :palettes/order]))

(defn palette-id-seq [state]
  (or (seq (order state))
      (keys (by-id state))))

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
