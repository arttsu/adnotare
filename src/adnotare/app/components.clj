(ns adnotare.app.components)

(defn section
  [label content & {:keys [section-props header-right] :or {section-props {}}}]
  (merge
   {:fx/type :v-box
    :style-class ["section"]
    :spacing 8
    :children [{:fx/type :h-box
                :children
                (cond-> [{:fx/type :label
                          :text label
                          :style-class ["section-label"]}
                         {:fx/type :region
                          :h-box/hgrow :always}]
                  header-right (conj header-right))}
               content]}
   section-props))
