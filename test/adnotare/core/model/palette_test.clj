(ns adnotare.core.model.palette-test
  (:require
   [adnotare.core.model.palette :as subject]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.util.uuid :as uuid]
   [adnotare.test.core.constants :as C]
   [clojure.test :refer [deftest is testing]]))

(deftest ordered-prompts-test
  (let [prompts (subject/ordered-prompts C/palette-1)]
    (is (= [[(uuid/named "prompt-11") "Comment"]
            [(uuid/named "prompt-15") "User answer"]
            [(uuid/named "prompt-14") "Give example"]
            [(uuid/named "prompt-12") "Explain"]
            [(uuid/named "prompt-13") "Provide evidence"]]
           (map (fn [[id prompt]] [id (::prompt/text prompt)]) prompts))))
  (testing "empty palette"
    (is (= [] (subject/ordered-prompts {::subject/label "Empty"
                                        ::subject/prompts {:by-id {} :order []}})))))
