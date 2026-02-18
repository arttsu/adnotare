(ns adnotare.core.model.document-test
  (:require
   [adnotare.core.model.annotation :as annotation]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.document :as subject]
   [adnotare.core.model.selection :as selection]
   [adnotare.core.util.uuid :as uuid]
   [adnotare.test.core.constants :as C]
   [clojure.test :refer [deftest is]]))

(deftest annotations-test
  (let [annotations (subject/annotations (::app/document C/default-app))]
    (is (= [[(uuid/named "annotation-1") "This is a test"]
            [(uuid/named "annotation-2") "Adnotare"]]
           (map (fn [[id annotation]] [id (get-in annotation [::annotation/selection ::selection/quote])])
                annotations)))))
