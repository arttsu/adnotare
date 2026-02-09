(ns adnotare.fx.extensions.code-area
  (:require [cljfx.api :as fx]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.mutator :as mutator]
            [cljfx.prop :as prop])
  (:import (java.util ArrayList Collection Collections)
           (javafx.scene.control IndexRange)
           (org.fxmisc.flowless VirtualizedScrollPane)
           (org.fxmisc.richtext CodeArea)
           (org.fxmisc.richtext.model StyleSpansBuilder)))

(defn- ->style-collection ^Collection [active-classes]
  (if (empty? active-classes)
    (Collections/emptyList)
    (ArrayList. active-classes)))

(defn- ->style-spans [{:keys [text spans]}]
  (let [n (count text)
        events (reduce (fn [acc {:keys [start end style-classes]}]
                         (-> acc
                             (update start (fnil concat []) (for [cls style-classes] [:add cls]))
                             (update end (fnil concat []) (for [cls style-classes] [:rem cls]))))
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

(defn pane->area ^CodeArea [^VirtualizedScrollPane pane]
  (let [user-data (.getUserData pane)]
    (when (instance? CodeArea user-data)
      user-data)))

(defn pane->selection [^VirtualizedScrollPane pane]
  (let [^CodeArea area (pane->area pane)
        ^IndexRange selection (.getSelection area)
        start (.getStart selection)
        end (.getEnd selection)
        text (.getSelectedText area)]
    {:start start :end end :text text}))

(defn clear-selection! [^VirtualizedScrollPane pane]
  (let [^CodeArea area (pane->area pane)]
    (.deselect area)))

(defn reveal-range! [^VirtualizedScrollPane pane selection]
  (let [^CodeArea area (pane->area pane)]
    (doto area
      (.selectRange (:start selection) (:end selection))
      (.requestFollowCaret)
      (.deselect))))

(def ^:private code-area-type
  (fx/make-ext-with-props
   {:code-area/model
    (prop/make (mutator/setter
                (fn [^VirtualizedScrollPane pane model]
                  (when-let [^CodeArea area (pane->area pane)]
                    (let [new-text (:text model)
                          old-text (.getText area)]
                      (when-not (= new-text old-text)
                        (if (.isEmpty new-text)
                          (.clear area)
                          (.replaceText area new-text)))
                      (when-not (.isEmpty new-text)
                        (let [spans (->style-spans model)]
                          (.setStyleSpans area 0 spans)))))))
               lifecycle/scalar)
    :code-area/read-only?
    (prop/make (mutator/setter
                (fn [^VirtualizedScrollPane pane ro?]
                  (when-let [^CodeArea area (pane->area pane)]
                    (.setEditable area (not (true? ro?))))))
               lifecycle/scalar)}))

(defn- ->code-area []
  (let [area (doto (CodeArea.)
               (.setWrapText true))
        pane (VirtualizedScrollPane. area)]
    (.setUserData pane area)
    pane))

(defn code-area [{:keys [code-area/model code-area/read-only?]}]
  {:fx/type code-area-type
   :desc {:fx/type fx/ext-instance-factory
          :create ->code-area}
   :props {:code-area/model model
           :code-area/read-only? read-only?}})

