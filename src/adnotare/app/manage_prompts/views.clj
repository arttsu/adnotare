(ns adnotare.app.manage-prompts.views
  (:require
   [adnotare.app.manage-prompts.subs :as subs])
  (:import
   (javafx.scene.control OverrunStyle)))

(defn- section [{:keys [title content]}]
  {:fx/type :v-box
   :style-class ["section"]
   :spacing 8
   :children [{:fx/type :label
               :text title
               :style-class ["section-label"]}
              content]})

(defn- palette-item [{:keys [id label active?]}]
  (cond-> {:fx/type :h-box
           :alignment :center-left
           :padding 8
           :on-mouse-clicked {:event/type :manage-prompts/select-palette
                              :palette-id id}
           :children [(cond-> {:fx/type :label
                               :text label}
                        active? (assoc :style "-fx-font-weight: bold;"))]}
    active? (assoc :style "-fx-background-color: #e9eef6; -fx-background-radius: 6;")))

(defn- palettes-pane [{:keys [fx/context]}]
  (let [active-id (subs/palette-id context)
        palettes (subs/palette-options context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :vbar-policy :as-needed
     :content {:fx/type :v-box
               :spacing 6
               :children (map (fn [{:keys [id] :as palette}]
                                {:fx/type palette-item
                                 :id id
                                 :label (:label palette)
                                 :active? (= id active-id)})
                              palettes)}}))

(defn- prompt-item [{:keys [id text color selected?]}]
  (cond-> {:fx/type :h-box
           :alignment :center-left
           :spacing 8
           :padding 8
           :on-mouse-clicked {:event/type :manage-prompts/select-prompt
                              :prompt-id id}
           :children [{:fx/type :region
                       :min-width 10
                       :min-height 10
                       :pref-width 10
                       :pref-height 10
                       :max-width 10
                       :max-height 10
                       :style-class [(str "color-" color)]}
                      {:fx/type :label
                       :text text
                       :max-width Double/MAX_VALUE
                       :h-box/hgrow :always
                       :wrap-text false
                       :text-overrun OverrunStyle/ELLIPSIS}]}
    selected? (assoc :style "-fx-background-color: #eef6ed; -fx-background-radius: 6;")))

(defn- prompts-pane [{:keys [fx/context]}]
  (let [selected-id (subs/selected-prompt-id context)
        prompts (or (subs/active-prompts context) [])]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :vbar-policy :as-needed
     :content {:fx/type :v-box
               :spacing 6
               :children (map (fn [{:keys [id] :as prompt}]
                                {:fx/type prompt-item
                                 :id id
                                 :text (:text prompt)
                                 :color (:color prompt)
                                 :selected? (= id selected-id)})
                              prompts)}}))

(defn- prompt-details [{:keys [fx/context]}]
  (if-let [{:keys [text color]} (subs/selected-prompt context)]
    {:fx/type :v-box
     :spacing 10
     :children [{:fx/type :h-box
                 :spacing 8
                 :alignment :center-left
                 :children [{:fx/type :label
                             :text "Color"}
                            {:fx/type :region
                             :min-width 12
                             :min-height 12
                             :pref-width 12
                             :pref-height 12
                             :max-width 12
                             :max-height 12
                             :style-class [(str "color-" color)]}]}
                {:fx/type :label
                 :text "Prompt text"}
                {:fx/type :label
                 :text text
                 :wrap-text true}]}
    {:fx/type :stack-pane
     :children [{:fx/type :label
                 :text "Select a prompt"}]}))

(defn root [_]
  {:fx/type :v-box
   :padding 14
   :spacing 12
   :children [{:fx/type :h-box
               :alignment :center-left
               :spacing 12
               :children [{:fx/type :button
                           :text "\u2190 Back"
                           :on-action {:event/type :app/navigate
                                       :route :annotate}}
                          {:fx/type :label
                           :text "Manage prompts"
                           :style-class ["section-label"]}]}
              {:fx/type :split-pane
               :v-box/vgrow :always
               :divider-positions [0.25 0.67]
               :items [{:fx/type :v-box
                        :padding 10
                        :children [{:fx/type section
                                    :title "Palettes"
                                    :content {:fx/type palettes-pane}}]}
                       {:fx/type :v-box
                        :padding 10
                        :children [{:fx/type section
                                    :title "Prompts"
                                    :content {:fx/type prompts-pane}}]}
                       {:fx/type :v-box
                        :padding 10
                        :children [{:fx/type section
                                    :title "Prompt details"
                                    :content {:fx/type prompt-details}}]}]}]})
