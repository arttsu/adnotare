(ns adnotare.rich
  (:require [cljfx.api :as fx]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.mutator :as mutator]
            [cljfx.prop :as prop])
  (:import (org.fxmisc.richtext CodeArea)
           (org.fxmisc.richtext.model StyleSpansBuilder)
           (java.util ArrayList Collection Collections)))

(defn- ->style-collection ^Collection [active-classes]
  (if (empty? active-classes)
    (Collections/emptyList)
    (ArrayList. active-classes)))

(defn- ->style-spans [{:keys [text spans]}]
  (let [n (count text)
        events (reduce (fn [acc {:keys [start end color selected]}]
                         (let [cls (if selected "ann-selected" (str "ann-" color))]
                           (-> acc
                               (update start (fnil conj []) [:add cls])
                               (update end (fnil conj []) [:rem cls]))))
                       {}
                       spans)
        idxs (->> (concat [0 n] (keys events)) distinct sort)
        builder (StyleSpansBuilder.)]
    (loop [prev (first idxs)
           active #{}
           more (rest idxs)]
      (if (empty? more)
        (.create builder)
        (let [i (first more)
              span-len (- i prev)
              active' (reduce (fn [acc [op cls]]
                                (case op
                                  :add (conj acc cls)
                                  :rem (disj acc cls)))
                              active
                              (get events prev))]
          (when (pos? span-len)
            (.add builder (->style-collection active') span-len))
          (recur i active' (rest more)))))))

(def ^:private code-area
  (fx/make-ext-with-props
   {:adnotare/model (prop/make (mutator/setter
                                (fn [^CodeArea area model]
                                  (.replaceText area (:text model))
                                  (.setStyleSpans area 0 (->style-spans model))))
                               lifecycle/scalar)
    :adnotare/read-only? (prop/make (mutator/setter
                                     (fn [^CodeArea area ro?]
                                       (.setEditable area (not (true? ro?)))))
                                    lifecycle/scalar)}))

(defn annotated-area [{:keys [adnotare/model]}]
  {:fx/type code-area
   :desc {:fx/type fx/ext-instance-factory
          :create #(doto (CodeArea.)
                     (.setWrapText true))}
   :props {:adnotare/model model
           :adnotare/read-only? true}})
