(ns adnotare.model.persistence
  (:require
   [adnotare.model.schema :as S]
   [adnotare.model.session :as session]
   [adnotare.model.state :as state]
   [adnotare.util.fs :as fs]
   [clojure.java.io :as io]
   [malli.core :as m]))

(defn- session-path [parent-dir]
  (str (io/file parent-dir ".adnotare" "session.edn")))

(defn load-state
  ([] (load-state (System/getProperty "user.home")))
  ([parent-dir]
   (let [persisted-session (fs/read-edn-file (session-path parent-dir))]
     (if (and (map? persisted-session) (= 1 (:version persisted-session)) (m/validate S/PersistedSession (:session persisted-session)))
       (-> state/default
           (assoc-in [:state/session :palettes] (get-in persisted-session [:session :palettes]))
           (update-in [:state/session] session/activate-last-used-palette))
       state/default))))
(m/=> load-state [:function
                  [:-> S/State]
                  [:-> :string S/State]])
