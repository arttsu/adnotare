(ns adnotare.main
  (:require [cljfx.api :as fx]
            [adnotare.handler :as handler]
            [adnotare.views :as views]))

(def renderer
  (fx/create-renderer
   :middleware (comp
                fx/wrap-context-desc
                (fx/wrap-map-desc (fn [_] {:fx/type views/root})))
   :opts {:fx.opt/map-event-handler handler/event-handler
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& _args]
  (fx/mount-renderer handler/*state renderer))
