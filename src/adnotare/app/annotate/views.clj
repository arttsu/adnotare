(ns adnotare.app.annotate.views
  (:require [adnotare.app.annotate.subs :as subs]
            [adnotare.app.node-registry :refer [registered]]
            [adnotare.fx.extensions.code-area :refer [code-area]])
  (:import (javafx.geometry Pos)
           (javafx.scene.control OverrunStyle)))

(defn- document [{:keys [fx/context]}]
  (registered
   :annotate/doc
   {:fx/type code-area
    :code-area/model (subs/doc-rich-text context)
    :code-area/read-only? true}))

(defn- prompt-button [{:keys [id text color]}]
  {:fx/type :button
   :text text
   :tooltip {:fx/type :tooltip :text text}
   :alignment Pos/CENTER
   :wrap-text false
   :text-overrun OverrunStyle/ELLIPSIS
   :max-width Double/MAX_VALUE
   :style-class ["prompt-btn" (str "color-" color)]
   :pref-height 44
   :on-action {:event/type :annotate/add-annotation :prompt-id id}})

(defn- prompt-pane [{:keys [fx/context]}]
  (let [prompts (subs/active-prompts context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :pannable true
     :hbar-policy :never
     :vbar-policy :as-needed
     :max-height 500
     :content {:fx/type :tile-pane
               :pref-columns 2
               :hgap 10
               :vgap 10
               :padding 10
               :pref-tile-width 260
               :children (map prompt-button prompts)}}))

(defn- annotation-list-item [{:keys [id selection prompt selected?]}]
  {:fx/type :h-box
   :alignment :center-left
   :padding 10
   :spacing 10
   :style-class (cond-> ["ann-list-item"]
                  selected? (conj "selected"))
   :on-mouse-clicked {:event/type :annotate/select-annotation :id id}
   :children [{:fx/type :region
               :min-width 12 :min-height 12
               :pref-width 12 :pref-height 12
               :max-width 12 :max-height 12
               :style-class [(str "color-" (:color prompt))]}
              {:fx/type :label
               :text (:text selection)
               :max-width Double/MAX_VALUE
               :h-box/hgrow :always
               :text-overrun OverrunStyle/ELLIPSIS}
              {:fx/type :button
               :text "x"
               :style-class ["ann-list-item-delete"]
               :on-mouse-clicked {:event/type :annotate/delete-annotation
                                  :id id}}]})

(defn- annotation-list [{:keys [fx/context]}]
  {:fx/type :scroll-pane
   :fit-to-width true
   :hbar-policy :never
   :vbar-policy :as-needed
   :max-height 600
   :content {:fx/type :v-box
             :padding 8
             :spacing 8
             :children (map annotation-list-item (subs/annotations context))}})

(defn- annotation-note-input [{:keys [fx/context]}]
  (let [disabled? (nil? (subs/selected-annotation context))
        note (subs/selected-annotation-note context)]
    (registered
     :annotate/selected-annotation-note
     {:fx/type :text-area
      :text note
      :disable disabled?
      :wrap-text true
      :pref-row-count 6
      :on-text-changed {:event/type :annotate/update-selected-annotation-note}})))

(defn root [_]
  {:fx/type :split-pane
   :divider-positions [0.6]
   :items
   [{:fx/type :v-box
     :padding 10
     :spacing 10
     :children
     [{:fx/type document
       :v-box/vgrow :always
       :max-height Double/MAX_VALUE}
      {:fx/type :h-box
       :padding 10
       :spacing 10
       :children [{:fx/type :button
                   :text "Paste"
                   :on-action {:event/type :annotate/paste-doc}}
                  {:fx/type :button
                   :text "Copy annotations"
                   :on-action {:event/type :annotate/copy-annotations}}]}]}
    {:fx/type :v-box
     :padding 10
     :spacing 10
     :children
     [{:fx/type :v-box
       :children
       [{:fx/type :label :text "Prompts"}
        {:fx/type prompt-pane}
        {:fx/type :label :text "Annotations"}
        {:fx/type annotation-list}
        {:fx/type :label :text "Annotation note"}
        {:fx/type annotation-note-input}]}]}]})
