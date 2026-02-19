(ns adnotare.app.views
  (:require
   [adnotare.app.annotator.views :as annotator.views]
   [adnotare.app.prompt-manager.views :as prompt-manager.views]
   [adnotare.app.subs :as subs]
   [adnotare.core.model.app :as model.app]
   [adnotare.core.model.toast :as toast]
   [adnotare.util.resources :as resources]))

(def ^:private toast-type->icon
  {::toast/success "OK"
   ::toast/warning "!"
   ::toast/error "X"
   ::toast/info "i"})

(defn- toast [[_id {::toast/keys [type text]}]]
  {:fx/type :h-box
   :style-class ["toast" (name type)]
   :padding 10
   :spacing 10
   :alignment :center-left
   :children
   [{:fx/type :region
     :style-class ["toast-accent" (name type)]
     :min-width 4
     :pref-width 4
     :max-width 4}
    {:fx/type :label
     :text (type toast-type->icon)
     :style-class ["toast-icon" (name type)]
     :min-width 18
     :alignment :center}
    {:fx/type :label
     :text text
     :max-width 360}]})

(defn- toasts [{:keys [fx/context]}]
  (let [toasts (subs/toasts context)]
    {:fx/type :v-box
     :pick-on-bounds false
     :alignment :bottom-center
     :spacing 8
     :padding 18
     :fill-width false
     :visible (any? toasts)
     :children
     (map toast toasts)}))

(defn- app [{:keys [fx/context]}]
  (let [route (subs/route context)]
    (case route
      ::model.app/annotator {:fx/type annotator.views/root}
      ::model.app/prompt-manager {:fx/type prompt-manager.views/root})))

(defn- initializing-view [_]
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
  (let [loaded? (subs/initialized? context)]
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
       :style-class ["app-root" "theme-zine"]
       :on-key-pressed {:event/type :hotkeys/key-pressed}
       :children
       [(if loaded?
          {:fx/type app}
          {:fx/type initializing-view})
        {:fx/type toasts
         :stack-pane/alignment :bottom-center}]}}}))
