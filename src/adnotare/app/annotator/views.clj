(ns adnotare.app.annotator.views
  (:require
   [adnotare.app.annotator.subs :as subs]
   [adnotare.app.components :refer [empty-state hotkey-chip label-with-hotkey section]]
   [adnotare.app.hotkeys :as hotkeys]
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
      {:fx/type :h-box
       :alignment :center-left
       :spacing 8
       :children
       [{:fx/type :combo-box
         :items options
         :value selected
         :max-width Double/MAX_VALUE
         :h-box/hgrow :always
         :button-cell #(select-keys % [:text])
         :cell-factory
         {:fx/cell-type :list-cell
          :describe #(select-keys % [:text])}
         :on-action {:event/type :annotator/switch-palette}}
        (hotkey-chip (str "Prev " (hotkeys/hotkey-label ::hotkeys/palette-prev)))
        (hotkey-chip (str "Next " (hotkeys/hotkey-label ::hotkeys/palette-next)))]}]}))

(defn- prompt-button [idx [id {::prompt/keys [label color]}]]
  (let [hotkey (hotkeys/prompt-hotkey-label idx)
        tooltip-text (if hotkey
                       (str label " (" hotkey ")")
                       label)]
    {:fx/type :button
     :graphic (if hotkey
                (label-with-hotkey label hotkey)
                {:fx/type :label :text label})
     :tooltip {:fx/type :tooltip :text tooltip-text}
     :wrap-text false
     :text-overrun OverrunStyle/ELLIPSIS
     :style-class ["prompt-btn" (str "color-" color)]
     :on-action {:event/type :annotator/add-annotation-from-selection :prompt-id id}}))

(defn- prompt-pane [{:keys [fx/context]}]
  (let [prompts (subs/active-prompts context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :content
     {:fx/type :flow-pane
      :hgap 10
      :vgap 10
      :padding 10
      :children (map-indexed prompt-button prompts)}}))

(defn- hotkey-action-button [label hotkey style-class event-type]
  {:fx/type :button
   :style-class style-class
   :graphic (label-with-hotkey label hotkey)
   :on-action {:event/type event-type}})

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
                              {{::prompt/keys [color] prompt-label ::prompt/label} ::annotation/prompt
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
       :text prompt-label
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
       :tape? true
       :style-classes ["section--primary"]
       :section-props {:v-box/vgrow :always})
      (section
       nil
       {:fx/type :flow-pane
        :padding 12
        :hgap 10
        :vgap 10
        :children
        [(hotkey-action-button
          "📥 Paste from clipboard"
          (hotkeys/hotkey-label ::hotkeys/paste)
          ["btn" "btn-primary" "btn-paste"]
          :annotator/paste-document-from-clipboard)
         (hotkey-action-button
          "📋 Copy annotations"
          (hotkeys/hotkey-label ::hotkeys/copy-annotations)
          ["btn" "btn-primary" "btn-copy"]
          :annotator/copy-annotations-as-llm-prompt)
         (hotkey-action-button
          "📋 Copy annotations with document"
          (hotkeys/hotkey-label ::hotkeys/copy-annotations+document)
          ["btn" "btn-primary" "btn-copy"]
          :annotator/copy-annotations-and-document-as-llm-prompt)]})]}
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
       :tape? true
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
       :tape? true
       :section-props {:v-box/vgrow :always
                       :min-height 0
                       :max-height Double/MAX_VALUE})]}]})
