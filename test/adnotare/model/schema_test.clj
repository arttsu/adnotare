(ns adnotare.model.schema-test
  (:require [adnotare.model.schema :as S]
            [adnotare.test.constants :as constants]
            [adnotare.test.schema :refer [is-valid]]
            [clojure.test :refer [deftest testing]]))

(deftest default-state
  (testing "conforms to schema"
    (is-valid S/State constants/default-state)))
