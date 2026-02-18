(ns adnotare.app.annotator.views
  (:require
   [adnotare.app.annotator.subs :as subs]
   [adnotare.app.components :refer [empty-state section]]
   [adnotare.app.node-registry :as node-registry]
   [adnotare.core.model.annotation :as annotation]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.model.selection :as selection]
   [adnotare.fx.extensions.code-area :refer [code-area]]
   [clojure.string :as string])
  (:import
   (javafx.scene.control OverrunStyle)))

(defn- document-empty? [context]
  (string/blank? (:text (subs/document-rich-text context))))

(defn- document [{:keys [fx/context]}]
  (if (document-empty? context)
    (empty-state
     "No document yet"
     "Paste text from the clipboard to start annotating.")
    (node-registry/registered
     :annotator/document-code-area
     {:fx/type code-area
      :code-area/model (subs/document-rich-text context)
      :code-area/read-only? true})))

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
   :pref-width 220
   :max-width 220
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
      :pref-tile-width 220
      :hgap 10
      :vgap 10
      :padding 10
      :children (map prompt-button prompts)}}))

(defn- prompt-section [{:keys [fx/context]}]
  (let [{:keys [options]} (subs/palette-selector-options context)]
    (cond
      (empty? options)
      (empty-state
       "No palettes"
       "Create a palette with some prompts in Edit Palettes to start annotating.")

      (empty? (subs/active-prompts context))
      {:fx/type :v-box
       :padding 12
       :spacing 10
       :children
       [{:fx/type palette-selector}
        (empty-state
         "No prompts yet"
         "This palette has no prompts. Add prompts in Edit Palettes.")]}

      :else
      {:fx/type :v-box
       :padding 12
       :spacing 10
       :children
       [{:fx/type palette-selector}
        {:fx/type prompt-pane}]})))

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
     :style-class ["ann-accent" (str "color-" color)]
     :min-width 5
     :pref-width 5
     :max-width 5
     :h-box/margin {:right 2}}
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
       :wrap-text false
       :text-overrun OverrunStyle/ELLIPSIS}]}
    {:fx/type :button
     :text "\u00d7"
     :style-class ["ann-list-item-delete"]
     :on-mouse-clicked {:event/type :annotator/delete-annotation :id id}}]})

(defn- annotation-list [{:keys [fx/context]}]
  (let [annotations (subs/annotations context)]
    (cond
      (document-empty? context)
      {:fx/type :stack-pane
       :max-width Double/MAX_VALUE
       :max-height Double/MAX_VALUE
       :children
       [(empty-state
         "No annotations yet"
         "Paste a document to enable annotation.")]}

      (empty? annotations)
      {:fx/type :stack-pane
       :max-width Double/MAX_VALUE
       :max-height Double/MAX_VALUE
       :children
       [(empty-state
         "No annotations yet"
         "Select text and press a prompt button to create one.")]}

      :else
      {:fx/type :scroll-pane
       :fit-to-width true
       :min-height 0
       :max-height Double/MAX_VALUE
       :content
       {:fx/type :v-box
        :padding 8
        :spacing 8
        :children (map annotation-list-item annotations)}})))

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
  {:fx/type :h-box
   :padding 10
   :spacing 10
   :children
   [{:fx/type :v-box
     :min-width 560
     :pref-width 900
     :h-box/hgrow :always
     :spacing 10
     :children
     [(section
       "Document"
       {:fx/type document
        :v-box/vgrow :always}
       :style-classes ["section--primary"]
       :section-props {:v-box/vgrow :always})
      (section
       nil
       {:fx/type :h-box
        :padding 12
        :spacing 10
        :children
        [{:fx/type :button
          :text "📥 Paste from clipboard"
          :style-class ["btn" "btn-primary" "btn-paste"]
          :on-action {:event/type :annotator/paste-document-from-clipboard}}
         {:fx/type :button
          :text "📋 Copy annotations"
          :style-class ["btn" "btn-primary" "btn-copy"]
          :on-action {:event/type :annotator/copy-annotations-as-llm-prompt}}
         {:fx/type :button
          :text "📋 Copy annotations with document"
          :style-class ["btn" "btn-primary" "btn-copy"]
          :on-action {:event/type :annotator/copy-annotations-and-document-as-llm-prompt}}]})]}
    {:fx/type :v-box
     :style-class ["side-panel"]
     :min-width 420
     :pref-width 560
     :max-width 700
     :min-height 0
     :spacing 10
     :children
     [(section
       "Prompts"
       {:fx/type prompt-section}
       :section-props {:min-height 360
                       :pref-height 360
                       :max-height 360}
       :header-right
       {:fx/type :button
        :text "Edit Palettes"
        :style-class ["btn"]
        :on-action {:event/type :ui/navigate, :route ::app/prompt-manager}})
      (section
       "Annotations"
       {:fx/type :v-box
        :v-box/vgrow :always
        :min-height 0
        :spacing 10
        :children
        [{:fx/type annotation-list
          :v-box/vgrow :always
          :min-height 0}
         {:fx/type selected-annotation-note-text-area}]}
       :section-props {:v-box/vgrow :always
                       :min-height 0
                       :max-height Double/MAX_VALUE})]}]})
