(ns adnotare.core.model.prompt-test
  (:require
   [adnotare.core.model.prompt :as subject]
   [clojure.test :refer [deftest is]]))

(deftest effective-text-test
  (is (= "Label only"
         (subject/effective-text {::subject/label "Label only"
                                  ::subject/instructions ""})))
  (is (= "Detailed instructions"
         (subject/effective-text {::subject/label "Label"
                                  ::subject/instructions "Detailed instructions"}))))
