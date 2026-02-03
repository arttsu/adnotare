(ns adnotare.util.resources
  (:require [clojure.java.io :as io]))

(defn url ^String [path]
  (some-> (io/resource path) str))
