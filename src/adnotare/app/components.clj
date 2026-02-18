(ns adnotare.app.components)

(defn empty-state [title subtitle]
  {:fx/type :v-box
   :style-class ["empty-state"]
   :alignment :center
   :spacing 4
   :children
   [{:fx/type :label
     :text title
     :style-class ["empty-state-title"]}
    {:fx/type :label
     :text subtitle
     :style-class ["empty-state-subtitle"]
     :wrap-text true}]})

(defn section
  [label content & {:keys [section-props header-right style-classes]
                    :or {section-props {}
                         style-classes []}}]
  (merge
   {:fx/type :v-box
    :style-class (into ["section"] style-classes)
    :spacing 10
    :children [{:fx/type :h-box
                :alignment :center-left
                :children
                (cond-> []
                  label (conj {:fx/type :label
                               :text label
                               :style-class ["section-label"]})
                  true (conj {:fx/type :region
                              :h-box/hgrow :always})
                  header-right (conj header-right))}
               content]}
   section-props))
