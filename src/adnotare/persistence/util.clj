(ns adnotare.persistence.util
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   [java.io PushbackReader]))

(defn- ensure-parent-dirs! [^java.io.File file]
  (when-let [parent (.getParentFile file)]
    (.mkdirs parent)))

(defn read-edn-file [path]
  (let [file (io/file path)]
    (if (.exists file)
      (with-open [reader (PushbackReader. (io/reader file))]
        (if-let [content (edn/read {:eof nil} reader)]
          {:status :ok, :value content}
          {:status :error, :reason :eof}))
      {:status :error, :reason :not-found})))

(defn write-edn-file! [path content]
  (let [file (io/file path)]
    (ensure-parent-dirs! file)
    (spit file (pr-str content))))
