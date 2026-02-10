(ns adnotare.model.persistence-test
  (:require
   [adnotare.model.persistence :as persistence]
   [adnotare.model.session :as session]
   [adnotare.model.state :as state]
   [adnotare.test.factory :as factory]
   [adnotare.util.fs :as fs]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]))

(defn- temp-dir []
  (-> (java.nio.file.Files/createTempDirectory "adnotare-persistence-test" (make-array java.nio.file.attribute.FileAttribute 0))
      (.toFile)
      (.getAbsolutePath)))

(defn- load-with [persisted]
  (let [parent-dir (temp-dir)
        path (str (java.io.File. parent-dir ".adnotare/session.edn"))]
    (when (some? persisted)
      (fs/write-edn-file! path persisted))
    (persistence/init-state parent-dir)))

(deftest load-state
  (testing "returns default state when no persisted data"
    (is (= {:status :ok
            :state (assoc-in state/default [:state/app :initialized?] true)}
           (load-with nil))))
  (testing "returns error when persisted data is not a map"
    (is (= {:status :error
            :state (assoc-in state/default [:state/app :initialized?] true)
            :reason "invalid session data"}
           (load-with "not-a-map"))))
  (testing "returns error when version is wrong"
    (let [palettes (get-in state/default [:state/session :palettes])]
      (is (= {:status :error
              :state (assoc-in state/default [:state/app :initialized?] true)
              :reason "invalid session data"}
             (load-with {:version 2 :session {:palettes palettes}})))))
  (testing "returns error when persisted session is invalid"
    (is (= {:status :error
            :state (assoc-in state/default [:state/app :initialized?] true)
            :reason "invalid session data"}
           (load-with {:version 1 :session {:palettes {:by-id {} :last-used-ms {"bad" 1}}}}))))
  (testing "loads palettes into default state and activates last used or first palette"
    (let [palettes (factory/->palettes
                    {:palettes
                     [(factory/->palette
                       {:label "Persisted"
                        :prompts
                        [(factory/->prompt {:text "Prompt 1"})
                         (factory/->prompt {:text "Prompt 2"})]})]})
          {:keys [status state]} (load-with {:version 1 :session {:palettes palettes}})]
      (is (= :ok status))
      (is (= "Persisted" (some-> state :state/session session/active-palette :label))))))

(deftest load-state-missing-app-dir
  (testing "returns default state when app directory does not exist"
    (let [parent-dir (str (temp-dir) "/does-not-exist")]
      (is (= {:status :ok
              :state (assoc-in state/default [:state/app :initialized?] true)}
             (persistence/init-state parent-dir))))))

(deftest load-state-malformed-edn
  (testing "returns error result with reason when persisted edn is malformed"
    (let [parent-dir (temp-dir)
          path (str (java.io.File. parent-dir ".adnotare/session.edn"))
          _ (.mkdirs (java.io.File. parent-dir ".adnotare"))
          result (do
                   (spit path "{:version 1 :session")
                   (persistence/init-state parent-dir))]
      (is (= :error (:status result)))
      (is (= (assoc-in state/default [:state/app :initialized?] true)
             (:state result)))
      (is (string? (:reason result)))
      (is (not (string/blank? (:reason result)))))))

(deftest persist-session!
  (testing "writes only palettes to session file with current version"
    (let [parent-dir (temp-dir)
          path (str (java.io.File. parent-dir ".adnotare/session.edn"))
          session (:state/session state/default)]
      (persistence/persist-session! parent-dir session)
      (is (= {:version 1
              :session {:palettes (:palettes session)}}
             (fs/read-edn-file path))))))
