(ns adnotare.core.state.ui.annotate)

(defn annotate-ui [state]
  (get-in state [:state/ui :ui/annotate]))

(defn active-palette-id [state]
  (get-in state [:state/ui :ui/annotate :annotate/active-palette-id]))

(defn set-active-palette [state palette-id]
  (assoc-in state [:state/ui :ui/annotate :annotate/active-palette-id] palette-id))

(defn selected-annotation-id [state]
  (get-in state [:state/ui :ui/annotate :annotate/selected-annotation-id]))

(defn select-annotation [state annotation-id]
  (assoc-in state [:state/ui :ui/annotate :annotate/selected-annotation-id] annotation-id))

(defn clear-annotation-selection [state]
  (select-annotation state nil))
