(ns adnotare.views
  (:require [adnotare.subs :as subs]
            [adnotare.rich :refer [annotated-area]]
            [clojure.java.io :as io])
  (:import [javafx.scene.control OverrunStyle]
           [javafx.geometry Pos]))

(defn resource-url ^String [path]
  (some-> (io/resource path) str))

(defn toast-banner [{:keys [fx/context]}]
  (let [{:keys [text]} (subs/toast context)
        visible? (boolean (seq text))]
    {:fx/type :h-box
     :style-class ["toast"]
     :padding 10
     :alignment :center-left
     :visible visible?
     :managed visible?
     :children [{:fx/type :label
                 :text text}]}))

(defn text [{:keys [fx/context adnotare/dispatch!]}]
  {:fx/type annotated-area
   :adnotare/model (subs/annotated-area-model context)
   :adnotare/dispatch! dispatch!})

(defn annotation-kind-button [id {:keys [color text]}]
  {:fx/type :button
   :text text
   :tooltip {:fx/type :tooltip
             :text text}
   :alignment Pos/CENTER
   :wrap-text false
   :text-overrun OverrunStyle/ELLIPSIS
   :max-width Double/MAX_VALUE
   :style-class ["ann-kind-btn" (str "ann-" color)]
   :pref-height 44
   :on-action  {:event/type :adnotare/add-annotation
                :adnotare/kind id}})

(defn annotation-kinds [{:keys [fx/context]}]
  (let [kinds (subs/annotation-kinds context)]
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
               :children (map (fn [[id kind]] (annotation-kind-button id kind)) kinds)}}))

(defn annotation-list-item [[id {:keys [text kind]}] kinds selected-id]
  (let [selected? (= id selected-id)
        color (get-in kinds [kind :color])]
    {:fx/type :h-box
     :alignment :center-left
     :spacing 10
     :padding 10
     :style-class (cond-> ["ann-list-item"]
                    selected? (conj "selected"))
     :on-mouse-clicked {:event/type :adnotare/select-annotation
                        :adnotare/id id}
     :children [{:fx/type :region
                 :min-width 12 :min-height 12
                 :pref-width 12 :pref-height 12
                 :max-width 12 :max-height 12
                 :style-class [(str "ann-" color)]}
                {:fx/type :label
                 :text text
                 :tooltip {:fx/type :tooltip :text text}
                 :max-width Double/MAX_VALUE
                 :h-box/hgrow :always
                 :text-overrun OverrunStyle/ELLIPSIS}
                {:fx/type :button
                 :text "x"
                 :style-class ["ann-list-item-delete"]
                 ;; Prevent clicking delete from also selecting via row click.
                 :on-mouse-clicked {:event/type :adnotare/consume-mouse-event}
                 :on-action {:event/type :adnotare/delete-annotation
                             :adnotare/id id}}]}))

(defn annotation-list [{:keys [fx/context]}]
  (let [annotations (subs/annotations context)
        kinds (subs/annotation-kinds context)
        selected-id (subs/selected-annotation-id context)]
    {:fx/type :scroll-pane
     :fit-to-width true
     :hbar-policy :never
     :vbar-policy :as-needed
     :max-height 220
     :content {:fx/type :v-box
               :spacing 8
               :padding 8
               :children (map (fn [a] (annotation-list-item a kinds selected-id)) annotations)}}))

(defn root [{:keys [:adnotare/dispatch!]}]
  {:fx/type :stage
   :showing true
   :title "Adnotare"
   :width 800
   :height 600
   :scene {:fx/type :scene
           :stylesheets [(resource-url "app.css")]
           :root {:fx/type :v-box
                  :children [{:fx/type toast-banner}
                             {:fx/type :split-pane
                              :divider-positions [0.70]
                              :items [{:fx/type :v-box
                                       :padding 10
                                       :spacing 10
                                       :children [{:fx/type text
                                                   :adnotare/dispatch! dispatch!}
                                                  {:fx/type :h-box
                                                   :spacing 10
                                                   :padding 10
                                                   :children [{:fx/type :button
                                                               :text "Paste"
                                                               :on-mouse-clicked {:event/type :adnotare/paste-text}}
                                                              {:fx/type :button
                                                               :text "Copy"
                                                               :on-mouse-clicked {:event/type :adnotare/copy-annotations}}]}]}
                                      {:fx/type :v-box
                                       :spacing 10
                                       :padding 10
                                       :children [{:fx/type :label
                                                   :text "Annotation kinds"}
                                                  {:fx/type annotation-kinds}
                                                  {:fx/type :separator}
                                                  {:fx/type :label
                                                   :text "Annotations"}
                                                  {:fx/type annotation-list}]}]}]}}})
