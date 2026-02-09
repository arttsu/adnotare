(ns adnotare.util.uuid
  (:import (java.util UUID)))

(defn random ^UUID []
  (UUID/randomUUID))

(defn named ^UUID [^String name]
  (UUID/nameUUIDFromBytes (.getBytes (str name) "UTF-8")))
