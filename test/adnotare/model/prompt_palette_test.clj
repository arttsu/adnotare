(ns adnotare.model.prompt-palette-test
  (:require [clojure.test :refer [deftest testing is]]
            [adnotare.test.schema :refer [is-valid]]
            [adnotare.test.constants :refer [default-prompt-palette]]
            [adnotare.model.prompt-palette :as palette]
            [adnotare.model.schema :as S]))

(deftest sorted-prompts
  (testing "returns ordered prompts"
    (let [prompts (palette/sorted-prompts default-prompt-palette)]
      (is-valid [:sequential S/DenormPrompt] prompts)
      (is (= (palette/prompt-order default-prompt-palette) (map :id prompts))))))
