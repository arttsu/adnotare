(ns adnotare.app.prompt-manager.views
  (:require
   [adnotare.app.components :refer [section]]
   [adnotare.app.prompt-manager.subs :as subs]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.prompt :as prompt]))

(defn- palette-list-item [[id {::palette/keys [label]}]]
  {:fx/type :h-box
   :alignment :center-left
   :padding 8
   :on-mouse-clicked {:event/type :prompt-manager/select-palette, :id id}
   :children
   [{:fx/type :label
     :text label}]})

(defn- palette-list [{:keys [fx/context]}]
  (let [palettes (subs/palettes context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :content
     {:fx/type :v-box
      :children
      (map palette-list-item palettes)}}))

(defn- prompt-list-item [[id {::prompt/keys [text]}]]
  {:fx/type :h-box
   :alignment :center-left
   :padding 8
   :on-mouse-clicked {:event/type :prompt-manager/select-prompt, :id id}
   :children
   [{:fx/type :label
     :text text}]})

(defn- prompt-list [{:keys [fx/context]}]
  (let [prompts (subs/prompts context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :content
     {:fx/type :v-box
      :children
      (map prompt-list-item prompts)}}))

(defn- palette-editor [{:keys [fx/context]}]
  (if-let [[_id palette] (subs/selected-palette context)]
    {:fx/type :v-box
     :spacing 10
     :children
     [{:fx/type :v-box
       :spacing 4
       :children
       [{:fx/type :label
         :text "Label"}
        {:fx/type :text-field
         :text (::palette/label palette)}]}
      {:fx/type :v-box
       :children
       [{:fx/type :label
         :text "Prompts"}
        {:fx/type prompt-list}]}]}
    {:fx/type :label
     :text "xXx"}))

(defn- prompt-editor [{:keys [fx/context]}]
  (if-let [[_id prompt] (subs/selected-prompt context)]
    {:fx/type :v-box
     :spacing 10
     :children
     [{:fx/type :v-box
       :spacing 4
       :children
       [{:fx/type :label
         :text "Text"}
        {:fx/type :text-field
         :text (::prompt/text prompt)}]}]}
    {:fx/type :label
     :text "xXx"}))

(defn root [_]
  {:fx/type :v-box
   :padding 14
   :spacing 12
   :children
   [{:fx/type :h-box
     :alignment :center-left
     :spacing 12
     :children
     [{:fx/type :button
       :text "\u2190 Back"
       :on-action {:event/type :ui/navigate :route ::app/annotator}}
      {:fx/type :label
       :text "Manage prompts"
       :style-class ["section-label"]}]}
    {:fx/type :split-pane
     :v-box/vgrow :always
     :divider-positions [0.25 0.67]
     :items
     [{:fx/type :v-box
       :padding 10
       :min-width 300
       :children
       [(section
         "Palettes"
         {:fx/type palette-list}
         :section-props
         {:max-height Double/MAX_VALUE
          :v-box/vgrow :always})]}
      {:fx/type :v-box
       :padding 10
       :min-width 300
       :children
       [(section
         "Palette Editor"
         {:fx/type palette-editor}
         :section-props
         {:max-height Double/MAX_VALUE
          :v-box/vgrow :always})]}
      {:fx/type :v-box
       :padding 10
       :min-width 300
       :children
       [(section
         "Prompt Editor"
         {:fx/type prompt-editor}
         :section-props
         {:max-height Double/MAX_VALUE
          :v-box/vgrow :always})]}]}]})
