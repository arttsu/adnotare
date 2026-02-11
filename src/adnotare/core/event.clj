(ns adnotare.core.event
  (:require
   [adnotare.core.schema :as S]
   [malli.core :as m]))

(defn result
  ([state]
   {:state state})
  ([state effect-map]
   (assoc effect-map :state state)))
(m/=> result
      [:function
       [:=> [:cat S/State] S/EventResult]
       [:=> [:cat S/State S/EffectMap] S/EventResult]])

(defmulti handle (fn [_state event] (:event/type event)))
(m/=> handle [:=> [:cat S/State S/Event] S/EventResult])

(defmethod handle :default [state _event]
  (result state))
