(ns adnotare.app.subs
  (:require [adnotare.model.ui :as ui]
            [cljfx.api :as fx]))

(defn- toasts [context]
  (fx/sub-val context ui/toasts))

(defn sorted-toasts [context]
  (sort-by :created-at-ms (fx/sub-ctx context toasts)))
