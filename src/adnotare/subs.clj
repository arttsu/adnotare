(ns adnotare.subs
  (:require [cljfx.api :as fx]
            [adnotare.rich :refer [annotations->style-spans]]))

(defn text [context]
  (fx/sub-val context :text))

(defn annotations [context]
  (fx/sub-val context :annotations))

(defn style-spans [context]
  (annotations->style-spans (text context) (annotations context)))
