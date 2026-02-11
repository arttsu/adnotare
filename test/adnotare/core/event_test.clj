(ns adnotare.core.event-test
  (:require
   [adnotare.core.event :as event]
   [adnotare.test.constants :refer [default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest app-start
  (testing "returns load-palettes effect"
    (let [result (event/handle default-state {:event/type :app/start})]
      (is (= {:on-load {:event/type :app/on-palettes-loaded}}
             (:load-palettes result))))))

(deftest annotate-delete-annotation
  (testing "clears selected id and forwards consume-event when provided"
    (let [fake-event {:kind :mouse}
          result (event/handle default-state {:event/type :annotate/delete-annotation
                                              :id (uuid/named "ann-2")
                                              :fx/event fake-event})]
      (is (nil? (get-in result [:state :state/ui :ui/annotate :annotate/selected-annotation-id])))
      (is (= fake-event (:consume-event result))))))

(deftest annotate-replace-doc
  (testing "clears annotations and selection"
    (let [result (event/handle default-state {:event/type :annotate/replace-doc :text "new"})]
      (is (= "new" (get-in result [:state :state/document :document/text])))
      (is (empty? (get-in result [:state :state/document :document/annotations :by-id])))
      (is (nil? (get-in result [:state :state/ui :ui/annotate :annotate/selected-annotation-id]))))))

(deftest annotate-switch-palette
  (testing "updates active palette and requests persistence"
    (let [palette-id (uuid/named "default-palette")
          result (event/handle default-state {:event/type :annotate/switch-palette
                                              :palette-id palette-id})]
      (is (= palette-id (get-in result [:state :state/ui :ui/annotate :annotate/active-palette-id])))
      (is (= (:state/palettes (:state result))
             (get-in result [:persist-palettes :palettes]))))))
