(ns adnotare.app.prompt-manager.views
  (:require
   [adnotare.app.components :refer [empty-state section]]
   [adnotare.app.node-registry :as node-registry]
   [adnotare.app.prompt-manager.subs :as subs]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.prompt :as prompt]))

(defn- palette-list-item [{:keys [selected-palette-id]} [id {::palette/keys [label]}]]
  {:fx/type :h-box
   :style-class (cond-> ["list-row"] (= id selected-palette-id) (conj "selected"))
   :alignment :center-left
   :padding 8
   :spacing 8
   :on-mouse-clicked {:event/type :prompt-manager/select-palette, :id id}
   :children
   [{:fx/type :label
     :text label}
    {:fx/type :region
     :h-box/hgrow :always}
    {:fx/type :button
     :text "×"
     :style-class ["list-item-delete"]
     :on-action {:event/type :prompt-manager/request-delete-palette :palette-id id}}]})

(defn- palette-list [{:keys [fx/context]}]
  (let [palettes (subs/palettes context)
        selected-palette-id (subs/selected-palette-id context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :content
     {:fx/type :v-box
      :children
      (map (partial palette-list-item {:selected-palette-id selected-palette-id}) palettes)}}))

(defn- prompt-list-item [{:keys [selected-prompt-id]} [id {::prompt/keys [label color]}]]
  {:fx/type :h-box
   :style-class (cond-> ["list-row"] (= id selected-prompt-id) (conj "selected"))
   :alignment :center-left
   :padding 8
   :spacing 8
   :on-mouse-clicked {:event/type :prompt-manager/select-prompt, :id id}
   :children
   [{:fx/type :region
     :style-class ["ann-accent" (str "color-" color)]
     :min-width 5
     :pref-width 5
     :max-width 5}
    {:fx/type :label
     :text label}
    {:fx/type :region
     :h-box/hgrow :always}
    {:fx/type :button
     :text "↑"
     :style-class ["list-item-order"]
     :on-action {:event/type :prompt-manager/move-prompt :prompt-id id :direction :up}}
    {:fx/type :button
     :text "↓"
     :style-class ["list-item-order"]
     :on-action {:event/type :prompt-manager/move-prompt :prompt-id id :direction :down}}
    {:fx/type :button
     :text "×"
     :style-class ["list-item-delete"]
     :on-action {:event/type :prompt-manager/request-delete-prompt :prompt-id id}}]})

(defn- prompt-list [{:keys [fx/context]}]
  (let [prompts (subs/prompts context)
        selected-prompt-id (subs/selected-prompt-id context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :content
     {:fx/type :v-box
      :children
      (map (partial prompt-list-item {:selected-prompt-id selected-prompt-id}) prompts)}}))

(defn- field-error [message]
  {:fx/type :label
   :text message
   :style-class ["field-error"]
   :visible (some? message)
   :managed (some? message)})

(defn- color-swatch [selected-color color]
  {:fx/type :button
   :text ""
   :style-class (cond-> ["color-swatch" (str "color-" color)]
                  (= selected-color color) (conj "selected"))
   :on-action {:event/type :prompt-manager/update-prompt-color :color color}})

(defn- palette-editor [{:keys [fx/context]}]
  (if-let [[palette-id _palette] (subs/selected-palette context)]
    (let [errors (subs/validation-errors context)]
      {:fx/type :v-box
       :spacing 10
       :children
       [{:fx/type :v-box
         :spacing 4
         :children
         [{:fx/type :label
           :text "Label"}
          (node-registry/registered
           :prompt-manager/palette-label
           {:fx/type :text-field
            :text (subs/draft-palette-label context)
            :on-text-changed {:event/type :prompt-manager/update-palette-label}})
          (field-error (:palette-label errors))]}
        {:fx/type :button
         :text "Delete palette"
         :style-class ["btn"]
         :on-action {:event/type :prompt-manager/request-delete-palette :palette-id palette-id}}
        {:fx/type :v-box
         :children
         [{:fx/type :label
           :text "Prompts"}
          {:fx/type prompt-list}]}
        {:fx/type :button
         :text "+ New prompt"
         :style-class ["btn"]
         :on-action {:event/type :prompt-manager/add-prompt}}]})
    (empty-state
     "Select a palette"
     "Choose a palette on the left to edit its prompts.")))

(defn- prompt-editor [{:keys [fx/context]}]
  (if-let [[prompt-id prompt] (subs/selected-prompt context)]
    (let [errors (subs/validation-errors context)]
      {:fx/type :v-box
       :spacing 10
       :children
       [{:fx/type :v-box
         :spacing 4
         :children
         [{:fx/type :label
           :text "Label"}
          (node-registry/registered
           :prompt-manager/prompt-label
           {:fx/type :text-field
            :text (subs/draft-prompt-label context)
            :on-text-changed {:event/type :prompt-manager/update-prompt-label}})
          (field-error (:prompt-label errors))]}
        {:fx/type :v-box
         :spacing 4
         :children
         [{:fx/type :label
           :text "Instructions"}
          {:fx/type :text-area
           :text (subs/draft-prompt-instructions context)
           :wrap-text true
           :pref-row-count 6
           :on-text-changed {:event/type :prompt-manager/update-prompt-instructions}}]}
        {:fx/type :v-box
         :spacing 6
         :children
         [{:fx/type :label
           :text "Color"}
          {:fx/type :flow-pane
           :hgap 8
           :vgap 8
           :children (map (partial color-swatch (::prompt/color prompt)) (range 10))}]}
        {:fx/type :h-box
         :spacing 8
         :children
         [{:fx/type :button
           :text "Move up"
           :style-class ["btn"]
           :on-action {:event/type :prompt-manager/move-prompt :prompt-id prompt-id :direction :up}}
          {:fx/type :button
           :text "Move down"
           :style-class ["btn"]
           :on-action {:event/type :prompt-manager/move-prompt :prompt-id prompt-id :direction :down}}
          {:fx/type :button
           :text "Delete prompt"
           :style-class ["btn"]
           :on-action {:event/type :prompt-manager/request-delete-prompt :prompt-id prompt-id}}]}]})
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
       :text "← Back"
       :style-class ["btn"]
       :on-action {:event/type :prompt-manager/navigate-back}}]}
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
         :tape? true
         :header-right
         {:fx/type :button
          :text "+ New palette"
          :style-class ["btn"]
          :on-action {:event/type :prompt-manager/add-palette}}
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
