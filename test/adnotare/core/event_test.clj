(ns adnotare.core.event-test
  (:require
   [adnotare.core.event :as event]
   [adnotare.test.constants :refer [default-state]]
   [clojure.test :refer [deftest is testing]]))

(deftest result-and-default-handle
  (testing "result arities"
    (is (= {:state default-state}
           (event/result default-state)))
    (is (= {:dispatch {:event/type :x} :state default-state}
           (event/result default-state {:dispatch {:event/type :x}}))))
  (testing "default handler returns unchanged state"
    (is (= {:state default-state}
           (event/handle default-state {:event/type :unknown/type})))))
