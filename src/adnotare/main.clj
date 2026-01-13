(ns adnotare.main
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [adnotare.events :as events]
            [adnotare.views :as views]))

(def *state
  (atom (fx/create-context {:text "Hello, World! This is a test of Adnotare."
                            :annotations [{:start 7 :end 12} {:start 32 :end 40}]}
                           cache/lru-cache-factory)))

(def event-handler
  (-> events/event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context (fx/make-reset-effect *state)})))

(def renderer
  (fx/create-renderer
   :middleware (comp
                fx/wrap-context-desc
                (fx/wrap-map-desc (fn [_] {:fx/type views/root})))
   :opts {:fx.opt/map-event-handler event-handler
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& _args]
  (fx/mount-renderer *state renderer))
