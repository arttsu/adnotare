(ns adnotare.fx.style-spans)

(defn- clamp [n x]
  (-> x (max 0) (min n)))

(defn- normalize-span [text-length {:keys [start end style-class]}]
  (let [start' (clamp text-length start)
        end' (clamp text-length end)
        classes (vec (distinct style-class))]
    (when (and (< start' end') (seq classes))
      {:start start' :end end' :style-class classes})))

(defn- add-class [{:keys [counts order]} cls]
  (let [count' (inc (get counts cls 0))]
    {:counts (assoc counts cls count')
     :order (if (= count' 1) (conj order cls) order)}))

(defn- remove-class [{:keys [counts order]} cls]
  (let [count (get counts cls 0)]
    (cond
      (<= count 0)
      {:counts counts :order order}

      (= count 1)
      {:counts (dissoc counts cls)
       :order (into [] (remove #(= % cls)) order)}

      :else
      {:counts (assoc counts cls (dec count))
       :order order})))

(defn- apply-events [active {:keys [add rem]}]
  ;; Process removals first so touching intervals [a,b) and [b,c) keep classes active at b.
  (let [after-rem (reduce remove-class active rem)]
    (reduce add-class after-rem add)))

(defn- append-run [runs run]
  (let [last-run (peek runs)]
    (if (and last-run (= (:style-class last-run) (:style-class run)))
      (conj (pop runs) (update last-run :len + (:len run)))
      (conj runs run))))

(defn style-runs [text spans]
  (let [text-length (count text)
        spans' (keep #(normalize-span text-length %) spans)
        events (reduce (fn [acc {:keys [start end style-class]}]
                         (-> acc
                             (update-in [start :add] (fnil into []) style-class)
                             (update-in [end :rem] (fnil into []) style-class)))
                       {}
                       spans')
        idxs (->> (concat [0 text-length] (keys events)) distinct sort)]
    (loop [prev (first idxs)
           active {:counts {} :order []}
           more (rest idxs)
           runs []]
      (if (empty? more)
        runs
        (let [i (first more)
              active' (apply-events active (get events prev))
              run-len (- i prev)
              runs' (if (pos? run-len)
                      (append-run runs {:len run-len :style-class (:order active')})
                      runs)]
          (recur i active' (rest more) runs'))))))
