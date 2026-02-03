(ns adnotare.model.schema-test
  (:require [clojure.test :refer [deftest testing]]
            [adnotare.test.constants :as constants]
            [adnotare.test.schema :refer [is-valid]]
            [adnotare.model.schema :as S]))

(deftest default-state
  (testing "conforms to schema"
    (is-valid S/State constants/default-state)))
