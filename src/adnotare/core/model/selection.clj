(ns adnotare.core.model.selection
  (:require
   [adnotare.core.util.schema :refer [Label]]))

(def Selection
  [:and
   [:map
    [::start [:int {:min 0}]]
    [::end :int]
    [::quote Label]]
   [:fn {:error/message "start must be < end"}
    (fn [{::keys [start end]}] (< start end))]
   [:fn {:error/message "text length must match end - start"}
    (fn [{::keys [start end quote]}] (= (count quote) (- end start)))]])
