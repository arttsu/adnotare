(ns adnotare.app.subs
  (:require [adnotare.model.app :as app]
            [cljfx.api :as fx]
            [malli.core :as m]
            [adnotare.model.schema :as S]))

(defn toasts [context]
  (fx/sub-val context (comp app/toasts :state/app)))
(m/=> toasts [:-> S/Context [:sequential S/Toast]])
