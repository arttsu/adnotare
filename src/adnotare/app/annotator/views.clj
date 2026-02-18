(ns adnotare.app.annotator.views
  (:require
   [adnotare.app.annotator.subs :as subs]
   [adnotare.app.components :refer [section]]
   [adnotare.app.node-registry :as node-registry]
   [adnotare.core.model.annotation :as annotation]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.model.selection :as selection]
   [adnotare.fx.extensions.code-area :refer [code-area]])
  (:import
   (javafx.scene.control OverrunStyle)))

(defn- document [{:keys [fx/context]}]
  (node-registry/registered
   :annotator/document-code-area
   {:fx/type code-area
    :code-area/model (subs/document-rich-text context)
    :code-area/read-only? true}))

(defn- palette-selector [{:keys [fx/context]}]
  (let [{:keys [options selected]} (subs/palette-selector-options context)]
    {:fx/type :v-box
     :spacing 4
     :children
     [{:fx/type :label
       :text "Palette"}
      {:fx/type :combo-box
       :items options
       :value selected
       :max-width Double/MAX_VALUE
       :button-cell #(select-keys % [:text])
       :cell-factory
       {:fx/cell-type :list-cell
        :describe #(select-keys % [:text])}
       :on-action {:event/type :annotator/switch-palette}}]}))

(defn- prompt-button [[id {::prompt/keys [text color]}]]
  {:fx/type :button
   :text text
   :tooltip {:fx/type :tooltip :text text}
   :wrap-text false
   :text-overrun OverrunStyle/ELLIPSIS
   :max-width Double/MAX_VALUE
   :style-class ["prompt-btn" (str "color-" color)]
   :on-action {:event/type :annotator/add-annotation-from-selection :prompt-id id}})

(defn- prompt-pane [{:keys [fx/context]}]
  (let [prompts (subs/active-prompts context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :content
     {:fx/type :tile-pane
      :pref-columns 2
      :pref-tile-width 260
      :hgap 10
      :vgap 10
      :padding 10
      :children (map prompt-button prompts)}}))

(defn- prompt-section [_]
  {:fx/type :v-box
   :padding 10
   :spacing 10
   :children
   [{:fx/type palette-selector}
    {:fx/type prompt-pane}]})

(defn- annotation-list-item [[id
                              {{::prompt/keys [color] prompt-text ::prompt/text} ::annotation/prompt
                               {::selection/keys [quote]} ::annotation/selection
                               ::annotation/keys [selected?]}]]
  {:fx/type :h-box
   :style-class (cond-> ["ann-list-item"] selected? (conj "selected"))
   :alignment :center-left
   :padding 10
   :spacing 10
   :on-mouse-clicked {:event/type :annotator/select-annotation :id id}
   :children
   [{:fx/type :region
     :style-class [(str "color-" color)]
     :min-width 12 :min-height 12
     :pref-width 12 :pref-height 12
     :max-width 12 :max-height 12}
    {:fx/type :v-box
     :spacing 4
     :h-box/hgrow :always
     :children
     [{:fx/type :label
       :text prompt-text
       :style-class ["ann-list-item-prompt"]
       :wrap-text false
       :text-overrun OverrunStyle/ELLIPSIS}
      {:fx/type :label
       :text quote
       :max-width Double/MAX_VALUE
       :wrap-text true
       :text-overrun OverrunStyle/ELLIPSIS}]}
    {:fx/type :button
     :text "x"
     :style-class ["ann-list-item-delete"]
     :on-mouse-clicked {:event/type :annotator/delete-annotation :id id}}]})

(defn- annotation-list [{:keys [fx/context]}]
  (let [annotations (subs/annotations context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :content
     (if (empty? annotations)
       {:fx/type :h-box
        :padding 10
        :alignment :center
        :children
        [{:fx/type :label
          :text "To add an annotation select some text and press one of the prompt buttons above"}]}
       {:fx/type :v-box
        :padding 8
        :spacing 8
        :children (map annotation-list-item annotations)})}))

(defn- selected-annotation-note-text-area [{:keys [fx/context]}]
  (let [visible? (subs/any-annotation-selected? context)]
    {:fx/type :v-box
     :visible visible?
     :spacing 4
     :children
     [{:fx/type :label
       :text "Additional note"}
      (node-registry/registered
       :annotator/selected-annotation-note-text-area
       {:fx/type :text-area
        :text (if visible? (subs/selected-annotation-note context) "")
        :disable (not visible?)
        :wrap-text true
        :pref-row-count 4
        :min-height 150
        :on-text-changed {:event/type :annotator/update-selected-annotation-note}})]}))

(defn root [_]
  {:fx/type :split-pane
   :divider-positions [0.6]
   :items
   [{:fx/type :v-box
     :padding 10
     :spacing 10
     :children
     [(section
       "Document"
       {:fx/type document
        :v-box/vgrow :always}
       :section-props {:v-box/vgrow :always})
      (section
       "Document actions"
       {:fx/type :h-box
        :padding 10
        :spacing 10
        :children
        [{:fx/type :button
          :text "Paste"
          :on-action {:event/type :annotator/paste-document-from-clipboard}}
         {:fx/type :button
          :text "Copy annotations"
          :on-action {:event/type :annotator/copy-annotations-as-llm-prompt}}
         {:fx/type :button
          :text "Copy annotations with document"
          :on-action {:event/type :annotator/copy-annotations-and-document-as-llm-prompt}}]})]}
    {:fx/type :border-pane
     :padding 10
     :top
     (section
      "Prompts"
      {:fx/type prompt-section}
      :header-right
      {:fx/type :button
       :text "Edit Palettes"
       :on-action {:event/type :ui/navigate, :route ::app/prompt-manager}})
     :center
     (section
      "Annotations"
      {:fx/type :v-box
       :spacing 10
       :children
       [{:fx/type annotation-list
         :v-box/vgrow :always}
        {:fx/type selected-annotation-note-text-area}]}
      :section-props {:border-pane/margin {:top 10}})}]})
