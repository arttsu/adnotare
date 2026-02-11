(ns adnotare.app.subs
  (:require [adnotare.model.app :as app]
            [adnotare.model.schema :as S]
            [cljfx.api :as fx]
            [malli.core :as m]))

(defn toasts [context]
  (fx/sub-val context (comp app/toasts :state/app)))
(m/=> toasts [:-> S/Context [:sequential S/Toast]])

(defn initialized? [context]
  (fx/sub-val context (comp :initialized? :state/app)))
(m/=> initialized? [:-> S/Context :boolean])

(defn route [context]
  (fx/sub-val context (comp :route :state/app)))
(m/=> route [:-> S/Context S/Route])
