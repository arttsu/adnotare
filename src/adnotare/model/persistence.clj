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

(defn init-state
  ([] (init-state (System/getProperty "user.home")))
  ([parent-dir]
   (try
     (let [persisted-session (fs/read-edn-file (session-path parent-dir))]
       (cond
         (nil? persisted-session)
         {:status :ok
          :state state/default}

         (and (map? persisted-session)
              (= 1 (:version persisted-session))
              (m/validate S/PersistedSession (:session persisted-session)))
         {:status :ok
          :state (-> state/default
                     (assoc-in [:state/session :palettes] (get-in persisted-session [:session :palettes]))
                     (update-in [:state/session] session/activate-last-used-palette))}

         :else
         {:status :error
          :state state/default
          :reason "invalid session data"}))
     (catch Exception e
       {:status :error
        :state state/default
        :reason (or (.getMessage e) "unknown error")}))))
(m/=> init-state [:function
                  [:-> S/InitStateResult]
                  [:-> :string S/InitStateResult]])

(defn persist-session!
  ([session]
   (persist-session! (System/getProperty "user.home") session))
  ([parent-dir session]
   (fs/write-edn-file!
    (session-path parent-dir)
    {:version 1
     :session {:palettes (:palettes session)}})))
(m/=> persist-session! [:function
                        [:-> S/Session :nil]
                        [:-> :string S/Session :nil]])
