(ns adnotare.rich
  (:require [cljfx.api :as fx]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.mutator :as mutator]
            [cljfx.prop :as prop]
            [adnotare.runtime :as rt]
            [adnotare.handler :as handler])
  (:import (org.fxmisc.richtext CodeArea)
           (org.fxmisc.richtext.model StyleSpansBuilder)
           (org.fxmisc.flowless VirtualizedScrollPane)
           (javafx.beans.value ChangeListener)
           (javafx.scene.control IndexRange)
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

(defn- install-selection-listener! [^CodeArea area]
  (.addListener (.selectionProperty area)
                (reify ChangeListener
                  (changed [_ _ _ _]
                    (let [^IndexRange r (.getSelection area)
                          start (.getStart r)
                          end (.getEnd r)
                          selected-text (.getSelectedText area)]
                      (handler/event-handler {:event/type :adnotare/rich-area-selection-changed
                                              :start start
                                              :end end
                                              :selected-text selected-text}))))))

(defn- pane->area ^CodeArea [^VirtualizedScrollPane pane]
  (let [ud (.getUserData pane)]
    (when (instance? CodeArea ud)
      ud)))

(def ^:private code-area-pane
  (fx/make-ext-with-props
   {:adnotare/model (prop/make (mutator/setter
                                (fn [^VirtualizedScrollPane pane model]
                                  (when-let [^CodeArea area (pane->area pane)]
                                    (.replaceText area (:text model))
                                    (.setStyleSpans area 0 (->style-spans model))
                                    (.moveTo area (min 100 (count (:text model)))))))
                               lifecycle/scalar)
    :adnotare/read-only? (prop/make (mutator/setter
                                     (fn [^VirtualizedScrollPane pane ro?]
                                       (when-let [^CodeArea area (pane->area pane)]
                                         (.setEditable area (not (true? ro?))))))
                                    lifecycle/scalar)}))

(defn- create-code-area-pane []
  (let [area (doto (CodeArea.)
               (.setWrapText true))
        pane (VirtualizedScrollPane. area)]
    (rt/set-rich-area! area)
    (install-selection-listener! area)
    (.setUserData pane area)
    pane))

(defn annotated-area [{:keys [adnotare/model]}]
  {:fx/type code-area-pane
   :desc {:fx/type fx/ext-instance-factory
          :create create-code-area-pane}
   :props {:adnotare/model model
           :adnotare/read-only? true}})
