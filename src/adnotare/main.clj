(ns adnotare.main
  (:require [cljfx.api :as fx]
            [adnotare.app.handler :refer [*state event-handler]]
            [adnotare.app.views :as views]))

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
  (fx/mount-renderer *state renderer))
