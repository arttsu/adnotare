(ns adnotare.main
  ;; PR: I'd like to find a better name for 'handler' ns.
  (:require [adnotare.app.handler :refer [*state event-handler]]
            [adnotare.app.views :as views]
            [cljfx.api :as fx]))

(defn- maybe-start-malli-dev! []
  (when (Boolean/parseBoolean (System/getProperty "adnotare.malli-dev" "false"))
    ((requiring-resolve 'malli.dev/start!))))

(def renderer
  (fx/create-renderer
   :middleware (comp
                fx/wrap-context-desc
                (fx/wrap-map-desc (fn [_] {:fx/type views/root})))
   :opts {:fx.opt/map-event-handler event-handler
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& _args]
  (maybe-start-malli-dev!)
  (fx/mount-renderer *state renderer)
  (fx/on-fx-thread
   (event-handler {:event/type :app/initialize})))
