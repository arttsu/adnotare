(ns adnotare.app.prompt-manager.views
  (:require
   [adnotare.app.components :refer [empty-state section]]
   [adnotare.app.prompt-manager.subs :as subs]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.prompt :as prompt]))

(defn- palette-list-item [[id {::palette/keys [label]}]]
  {:fx/type :h-box
   :style-class ["list-row"]
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
   :style-class ["list-row"]
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
    (empty-state
     "Select a palette"
     "Choose a palette on the left to edit its prompts.")))

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
    (empty-state
     "Select a prompt"
     "Choose a prompt from the palette to edit its text.")))

(defn root [_]
  {:fx/type :v-box
   :padding 14
   :spacing 12
   :children
   [{:fx/type :h-box
     :alignment :center-left
     :spacing 12
     :children
     [{:fx/type :label
       :text "Edit Prompt Palettes"
       :style-class ["prompt-manager-title"]}
      {:fx/type :region
       :h-box/hgrow :always}
      {:fx/type :button
       :text "\u2190 Back"
       :style-class ["btn"]
       :on-action {:event/type :ui/navigate :route ::app/annotator}}]}
    {:fx/type :h-box
     :v-box/vgrow :always
     :spacing 10
     :children
     [{:fx/type :v-box
       :min-width 340
       :pref-width 440
       :h-box/hgrow :always
       :children
       [(section
         "Palettes"
         {:fx/type palette-list}
         :section-props
         {:max-height Double/MAX_VALUE
          :v-box/vgrow :always})]}
      {:fx/type :v-box
       :min-width 340
       :pref-width 440
       :h-box/hgrow :always
       :children
       [(section
         "Palette Editor"
         {:fx/type palette-editor}
         :section-props
         {:max-height Double/MAX_VALUE
          :v-box/vgrow :always})]}
      {:fx/type :v-box
       :min-width 340
       :pref-width 440
       :h-box/hgrow :always
       :children
       [(section
         "Prompt Editor"
         {:fx/type prompt-editor}
         :section-props
         {:max-height Double/MAX_VALUE
          :v-box/vgrow :always})]}]}]})
