(ns adnotare.app.views
  (:require [adnotare.app.annotate.views :as annotate]
            [adnotare.app.manage-prompts.views :as manage-prompts]
            [adnotare.app.subs :as subs]
            [adnotare.util.resources :as resources]))

(def ^:private toast-icon-by-type
  {:success "OK"
   :warning "!"
   :error "X"
   :info "i"})

(defn- toast-banner [{:toast/keys [text type]}]
  {:fx/type :h-box
   :style-class ["toast" (name type)]
   :padding 10
   :alignment :center-left
   :spacing 10
   :children [{:fx/type :region
               :style-class ["toast-accent" (name type)]
               :pref-width 4
               :min-width 4
               :max-width 4}
              {:fx/type :label
               :style-class ["toast-icon" (name type)]
               :text (get toast-icon-by-type type "i")
               :min-width 18
               :alignment :center}
              {:fx/type :label
               :text text
               :max-width 360}]})

(defn- toast-list [{:keys [fx/context]}]
  (let [toasts (subs/toasts context)]
    {:fx/type :v-box
     :pick-on-bounds false
     :alignment :bottom-center
     :spacing 8
     :padding 18
     :fill-width false
     :visible (any? toasts)
     :children (map toast-banner toasts)}))

(defn- loading-view [_]
  {:fx/type :stack-pane
   :children
   [{:fx/type :v-box
     :alignment :center
     :spacing 12
     :children [{:fx/type :progress-indicator
                 :max-width 48
                 :max-height 48}
                {:fx/type :label
                 :text "Loading palettes..."}]}]})

(defn root [{:keys [fx/context]}]
  (let [initialized? (subs/initialized? context)
        route (subs/route context)]
    {:fx/type :stage
     :showing true
     :title "Adnotare"
     :width 1600
     :height 1200
     :on-close-request {:event/type :app/quit}
     :scene
     {:fx/type :scene
      :stylesheets [(resources/url "app.css")]
      :root
      {:fx/type :stack-pane
       :children
       [(if initialized?
          (case route
            :manage-prompts {:fx/type manage-prompts/root}
            :annotate {:fx/type annotate/root}
            {:fx/type annotate/root})
          {:fx/type loading-view})
        {:fx/type toast-list
         :stack-pane/alignment :bottom-center}]}}}))
