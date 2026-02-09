(ns adnotare.model.prompt-palette-test
  (:require [adnotare.model.prompt-palette :as palette]
            [adnotare.model.schema :as S]
            [adnotare.test.constants :refer [default-prompt-palette]]
            [adnotare.test.schema :refer [is-valid]]
            [clojure.test :refer [deftest testing is]]))

(deftest sorted-prompts
  (testing "returns ordered prompts"
    (let [prompts (palette/sorted-prompts default-prompt-palette)]
      (is-valid [:sequential S/DenormPrompt] prompts)
      (is (= (palette/prompt-order default-prompt-palette) (map :id prompts))))))
