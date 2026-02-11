(ns adnotare.core.persist.palettes
  (:require
   [adnotare.core.schema :as S]
   [adnotare.core.state :as state]
   [adnotare.util.fs :as fs]
   [clojure.java.io :as io]
   [malli.core :as m]))

(defn- palettes-path [parent-dir]
  (str (io/file parent-dir ".adnotare" "palettes.edn")))

(defn- migrate [persisted]
  (cond
    (not (map? persisted))
    {:status :error :reason "invalid palettes data"}

    (= 1 (:palettes/version persisted))
    (if (m/validate S/Palettes (:palettes/data persisted))
      {:status :ok :palettes (:palettes/data persisted)}
      {:status :error :reason "invalid palettes data"})

    :else
    {:status :error :reason "unknown palettes version"}))

(defn read-palettes
  ([] (read-palettes (System/getProperty "user.home")))
  ([parent-dir]
   (try
     (let [persisted (fs/read-edn-file (palettes-path parent-dir))]
       (if (nil? persisted)
         {:status :ok
          :palettes state/default-palettes}
         (let [{:keys [status palettes reason]} (migrate persisted)]
           (cond-> {:status status
                    :palettes (or palettes state/default-palettes)}
             (= :error status) (assoc :reason reason)))))
     (catch Exception e
       {:status :error
        :palettes state/default-palettes
        :reason (or (.getMessage e) "unknown error")}))))

(defn write-palettes!
  ([palettes]
   (write-palettes! (System/getProperty "user.home") palettes))
  ([parent-dir palettes]
   (fs/write-edn-file!
    (palettes-path parent-dir)
    {:palettes/version 1
     :palettes/data palettes})))
