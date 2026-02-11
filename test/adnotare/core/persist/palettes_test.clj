(ns adnotare.core.persist.palettes-test
  (:require
   [adnotare.core.persist.palettes :as persist.palettes]
   [adnotare.core.state :as state]
   [adnotare.test.factory :as factory]
   [adnotare.util.fs :as fs]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]))

(defn- temp-dir []
  (-> (java.nio.file.Files/createTempDirectory "adnotare-persist-palettes-test" (make-array java.nio.file.attribute.FileAttribute 0))
      (.toFile)
      (.getAbsolutePath)))

(deftest read-palettes
  (testing "returns defaults when no persisted data"
    (let [parent-dir (temp-dir)]
      (is (= {:status :ok
              :palettes state/default-palettes}
             (persist.palettes/read-palettes parent-dir)))))
  (testing "returns error when payload is invalid"
    (let [parent-dir (temp-dir)
          path (str (java.io.File. parent-dir ".adnotare/palettes.edn"))]
      (fs/write-edn-file! path "invalid")
      (is (= {:status :error
              :palettes state/default-palettes
              :reason "invalid palettes data"}
             (persist.palettes/read-palettes parent-dir)))))
  (testing "returns error when version is unknown"
    (let [parent-dir (temp-dir)
          path (str (java.io.File. parent-dir ".adnotare/palettes.edn"))]
      (fs/write-edn-file! path {:palettes/version 2 :palettes/data state/default-palettes})
      (is (= {:status :error
              :palettes state/default-palettes
              :reason "unknown palettes version"}
             (persist.palettes/read-palettes parent-dir)))))
  (testing "reads valid palettes"
    (let [palettes (factory/->palettes
                    {:palettes
                     [(factory/->palette
                       {:palette/label "Persisted"
                        :palette/prompts
                        [(factory/->prompt {:prompt/text "Prompt 1"})
                         (factory/->prompt {:prompt/text "Prompt 2"})]})]})
          parent-dir (temp-dir)
          path (str (java.io.File. parent-dir ".adnotare/palettes.edn"))]
      (fs/write-edn-file! path {:palettes/version 1 :palettes/data palettes})
      (is (= {:status :ok
              :palettes palettes}
             (persist.palettes/read-palettes parent-dir))))))

(deftest read-palettes-malformed-edn
  (testing "returns readable reason when edn is malformed"
    (let [parent-dir (temp-dir)
          path (str (java.io.File. parent-dir ".adnotare/palettes.edn"))
          _ (.mkdirs (java.io.File. parent-dir ".adnotare"))
          result (do
                   (spit path "{:palettes/version 1 :palettes/data")
                   (persist.palettes/read-palettes parent-dir))]
      (is (= :error (:status result)))
      (is (= state/default-palettes (:palettes result)))
      (is (string? (:reason result)))
      (is (not (string/blank? (:reason result)))))))

(deftest write-palettes
  (testing "writes persisted palettes format"
    (let [parent-dir (temp-dir)
          path (str (java.io.File. parent-dir ".adnotare/palettes.edn"))]
      (persist.palettes/write-palettes! parent-dir state/default-palettes)
      (is (= {:palettes/version 1
              :palettes/data state/default-palettes}
             (fs/read-edn-file path))))))
