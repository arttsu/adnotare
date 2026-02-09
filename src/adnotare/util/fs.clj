(ns adnotare.util.fs
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   (java.io PushbackReader)))

(defn- ensure-parent-dirs! [^java.io.File f]
  (when-let [p (.getParentFile f)]
    (.mkdirs p)))

(defn read-edn-file [path]
  (let [f (io/file path)]
    (when (.exists f)
      (with-open [r (PushbackReader. (io/reader f))]
        (edn/read {:eof nil} r)))))

(defn write-edn-file! [path x]
  (let [f (io/file path)]
    (ensure-parent-dirs! f)
    (spit f (pr-str x))))
