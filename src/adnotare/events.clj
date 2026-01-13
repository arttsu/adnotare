(ns adnotare.events
  (:require [cljfx.api :as fx]))

(defmulti event-handler :event/type)

(defmethod event-handler ::type-text [{:keys [fx/context fx/event]}]
  {:context (fx/swap-context context assoc :text event)})
