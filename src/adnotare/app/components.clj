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
  [label content & {:keys [section-props header-right style-classes tape?]
                    :or {section-props {}
                         style-classes []
                         tape? false}}]
  (let [header-label (when label
                       (if tape?
                         {:fx/type :stack-pane
                          :alignment :center-left
                          :h-box/margin {:right 8}
                          :children
                          [{:fx/type :region
                            :style-class ["tape"]
                            :min-height 18
                            :pref-height 18
                            :max-height 18}
                           {:fx/type :label
                            :text label
                            :style-class ["section-label" "section-label--taped"]}]}
                         {:fx/type :label
                          :text label
                          :style-class ["section-label"]}))]
  (merge
   {:fx/type :v-box
    :style-class (into ["section"] style-classes)
    :spacing 10
    :children [{:fx/type :h-box
                :alignment :center-left
                :children
                (cond-> []
                  header-label (conj header-label)
                  true (conj {:fx/type :region
                              :h-box/hgrow :always})
                  header-right (conj header-right))}
               content]}
   section-props)))
