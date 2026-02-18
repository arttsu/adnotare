(ns adnotare.core.model.app-test
  (:require
   [adnotare.core.model.app :as subject]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.util.uuid :as uuid]
   [adnotare.test.core.constants :as C]
   [adnotare.test.core.factory :as factory]
   [clojure.test :refer [deftest is]]))

(deftest prompt-by-ref-test
  (let [prompt (subject/prompt-by-ref C/default-app (factory/->prompt-ref (uuid/named "palette-1") (uuid/named "prompt-12")))]
    (is (= "Explain" (::prompt/text prompt)))))
