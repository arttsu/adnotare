(ns adnotare.persistence.palettes
  (:require
   [adnotare.core.model.palettes :refer [Palettes]]
   [adnotare.core.util.result :refer [ReadEDNFileResult]]
   [adnotare.persistence.util :refer [read-edn-file write-edn-file!]]
   [clojure.java.io :as io]
   [malli.core :as m]))

(defn- path []
  (io/file (System/getProperty "user.home") ".adnotare" "palettes.edn"))

(defn read-persisted []
  (read-edn-file (path)))
(m/=> read-persisted [:=> [:cat] ReadEDNFileResult])

(defn write-palettes! [palettes]
  (let [content {:version 1, :data palettes}]
    (write-edn-file! (path) content)))
(m/=> write-palettes! [:=> [:cat Palettes] :nil])
