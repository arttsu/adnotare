(ns adnotare.core.state.ui.annotate
  (:require
   [adnotare.core.schema :as S]
   [malli.core :as m]))

(defn annotate-ui [state]
  (get-in state [:state/ui :ui/annotate]))
(m/=> annotate-ui [:=> [:cat S/State] S/AnnotateUI])

(defn active-palette-id [state]
  (get-in state [:state/ui :ui/annotate :annotate/active-palette-id]))
(m/=> active-palette-id [:=> [:cat S/State] [:maybe :uuid]])

(defn set-active-palette [state palette-id]
  (assoc-in state [:state/ui :ui/annotate :annotate/active-palette-id] palette-id))
(m/=> set-active-palette [:=> [:cat S/State [:maybe :uuid]] S/State])

(defn selected-annotation-id [state]
  (get-in state [:state/ui :ui/annotate :annotate/selected-annotation-id]))
(m/=> selected-annotation-id [:=> [:cat S/State] [:maybe :uuid]])

(defn select-annotation [state annotation-id]
  (assoc-in state [:state/ui :ui/annotate :annotate/selected-annotation-id] annotation-id))
(m/=> select-annotation [:=> [:cat S/State [:maybe :uuid]] S/State])

(defn clear-annotation-selection [state]
  (select-annotation state nil))
(m/=> clear-annotation-selection [:=> [:cat S/State] S/State])
