(ns adnotare.test.schema
  (:require [clojure.test :refer [is]]
            [malli.core :as m]
            [malli.error :as me]))

(defn explain-str [schema x]
  (-> (m/explain schema x) (me/humanize) pr-str))

(defn is-valid [schema x]
  (is (m/validate schema x)
      (explain-str schema x)))
