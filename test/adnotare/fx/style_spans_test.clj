(ns adnotare.fx.style-spans-test
  (:require
   [adnotare.fx.style-spans :as subject]
   [clojure.test :refer [deftest is]]))

(deftest style-runs-disjoint-spans-test
  (is (= [{:len 2 :style-class ["a"]}
          {:len 2 :style-class []}
          {:len 2 :style-class ["b"]}
          {:len 4 :style-class []}]
         (subject/style-runs "abcdefghij"
                            [{:start 0 :end 2 :style-class ["a"]}
                             {:start 4 :end 6 :style-class ["b"]}]))))

(deftest style-runs-overlapping-spans-test
  (is (= [{:len 1 :style-class []}
          {:len 2 :style-class ["a"]}
          {:len 3 :style-class ["a" "b"]}
          {:len 2 :style-class ["b"]}
          {:len 2 :style-class []}]
         (subject/style-runs "abcdefghij"
                            [{:start 1 :end 6 :style-class ["a"]}
                             {:start 3 :end 8 :style-class ["b"]}]))))

(deftest style-runs-overlapping-same-class-test
  (is (= [{:len 4 :style-class ["a"]}
          {:len 3 :style-class ["a" "b"]}
          {:len 2 :style-class ["b"]}
          {:len 1 :style-class []}]
         (subject/style-runs "abcdefghij"
                            [{:start 0 :end 7 :style-class ["a"]}
                             {:start 2 :end 5 :style-class ["a"]}
                             {:start 4 :end 9 :style-class ["b"]}]))))

(deftest style-runs-touching-spans-test
  (is (= [{:len 6 :style-class ["a"]}
          {:len 2 :style-class []}]
         (subject/style-runs "abcdefgh"
                            [{:start 0 :end 3 :style-class ["a"]}
                             {:start 3 :end 6 :style-class ["a"]}]))))

(deftest style-runs-normalizes-invalid-ranges-test
  (is (= [{:len 2 :style-class ["a"]}
          {:len 3 :style-class ["b"]}]
         (subject/style-runs "abcde"
                            [{:start -2 :end 2 :style-class ["a"]}
                             {:start 2 :end 20 :style-class ["b"]}
                             {:start 4 :end 4 :style-class ["ignored"]}
                             {:start 1 :end 3 :style-class []}]))))
