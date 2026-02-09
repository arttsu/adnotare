(ns adnotare.model.persistence-test
  (:require [adnotare.model.persistence :as persistence]
            [adnotare.model.state :as state]
            [adnotare.util.fs :as fs]
            [adnotare.util.uuid :as uuid]
            [clojure.test :refer [deftest testing is]]
            [adnotare.test.factory :as factory]
            [adnotare.model.session :as session]))

(defn- temp-dir []
  (-> (java.nio.file.Files/createTempDirectory "adnotare-persistence-test" (make-array java.nio.file.attribute.FileAttribute 0))
      (.toFile)
      (.getAbsolutePath)))

(defn- load-with [persisted]
  (let [parent-dir (temp-dir)
        path (str (java.io.File. parent-dir ".adnotare/session.edn"))]
    (when (some? persisted)
      (fs/write-edn-file! path persisted))
    (persistence/load-state parent-dir)))

(deftest load-state
  (testing "returns default state when no persisted data"
    (is (= state/default (load-with nil))))
  (testing "returns default state when persisted data is not a map"
    (is (= state/default (load-with "not-a-map"))))
  (testing "returns default state when version is wrong"
    (let [palettes (get-in state/default [:state/session :palettes])]
      (is (= state/default
             (load-with {:version 2 :session {:palettes palettes}})))))
  (testing "returns default state when persisted session is invalid"
    (is (= state/default
           (load-with {:version 1 :session {:palettes {:by-id {} :last-used-ms {"bad" 1}}}}))))
  (testing "loads palettes into default state and activates last used or first palette"
    (let [palettes (factory/->palettes
                    {:palettes
                     [(factory/->palette
                       {:label "Persisted"
                        :prompts
                        [(factory/->prompt {:text "Prompt 1"})
                         (factory/->prompt {:text "Prompt 2"})]})]})
          loaded (load-with {:version 1 :session {:palettes palettes}})]
      (prn palettes)
      (is (= "Persisted" (some-> loaded :state/session session/active-palette :label))))))

(deftest load-state-missing-app-dir
  (testing "returns default state when app directory does not exist"
    (let [parent-dir (str (temp-dir) "/does-not-exist")]
      (is (= state/default (persistence/load-state parent-dir))))))
