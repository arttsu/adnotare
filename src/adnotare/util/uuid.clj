(ns adnotare.util.uuid
  (:import (java.util UUID)))

(defn new-uuid []
  (UUID/randomUUID))

(defn uuid ^UUID [^String name]
  (UUID/nameUUIDFromBytes (.getBytes (str name) "UTF-8")))
