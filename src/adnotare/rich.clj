(ns adnotare.rich
  (:require [cljfx.api :as fx]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.mutator :as mutator]
            [cljfx.prop :as prop])
  (:import (org.fxmisc.richtext CodeArea)
           (org.fxmisc.richtext.model StyleSpans StyleSpansBuilder)
           (java.util ArrayList Collection Collections)))

(defn ->style-collection ^Collection [active-classes]
  (if (empty? active-classes)
    (Collections/emptyList)
    (ArrayList. active-classes)))

(defn annotations->style-spans [text annotations]
  (let [n (count text)
        events (reduce (fn [acc {:keys [start end]}]
                         (let [cls "ann-yellow"]
                           (-> acc
                               (update start (fnil conj []) [:add cls])
                               (update end (fnil conj []) [:rem cls]))))
                       {}
                       annotations)
        idxs (->> (concat [0 n] (keys events)) distinct sort)
        builder (StyleSpansBuilder.)]
    (loop [prev (first idxs)
           active #{}
           more (rest idxs)]
      (if (empty? more)
        (.create builder)
        (let [i (first more)
              segment-len (- i prev)
              active' (reduce (fn [acc [op cls]]
                                (case op
                                  :add (conj acc cls)
                                  :rem (disj acc cls)))
                              active
                              (get events prev))]
          (when (pos? segment-len)
            (.add builder (->style-collection active') segment-len))
          (recur i active' (rest more)))))))

(def code-area
  (fx/make-ext-with-props
   {:adnotare/text (prop/make (mutator/setter
                               (fn [^CodeArea area ^String s]
                                 (.replaceText area s)))
                              lifecycle/scalar)
    :adnotare/style-spans (prop/make (mutator/setter
                                      (fn [^CodeArea area ^StyleSpans spans]
                                        (.setStyleSpans area 0 spans)))
                                     lifecycle/scalar)
    :adnotare/read-only? (prop/make (mutator/setter
                                     (fn [^CodeArea area ro?]
                                       (.setEditable area (not (true? ro?)))))
                                    lifecycle/scalar)}))
