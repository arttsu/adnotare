(ns adnotare.core.features.manage-prompts-test
  (:require
   [adnotare.core.features.manage-prompts :as subject]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.model.prompt-manager :as prompt-manager]
   [adnotare.core.util.uuid :as uuid]
   [adnotare.test.core.constants :as C]
   [clojure.test :refer [are deftest is]]))

(deftest palettes-test
  (is (= [[(uuid/named "palette-1") C/palette-1]
          [(uuid/named "palette-3") C/palette-3]
          [(uuid/named "palette-2") C/palette-2]]
         (subject/palettes C/default-app))))

(deftest selected-palette-test
  (are [result app] (is (= result (subject/selected-palette app)))
    nil C/default-app
    [(uuid/named "palette-3") C/palette-3] (assoc-in C/default-app
                                                     [::app/prompt-manager ::prompt-manager/selected-palette-id]
                                                     (uuid/named "palette-3"))))

(deftest selected-prompt-test
  (are [result app] (is (= result (subject/selected-prompt app)))
    nil C/default-app
    nil (assoc-in C/default-app
                  [::app/prompt-manager ::prompt-manager/selected-palette-id]
                  (uuid/named "palette-3"))
    [(uuid/named "prompt-31") {::prompt/text "Comment" ::prompt/color 0}]
    (-> C/default-app
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] (uuid/named "palette-3"))
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] (uuid/named "prompt-31")))))

((deftest select-palette-test
   (let [app-after (subject/select-palette (-> C/default-app
                                               (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] (uuid/named "palette-3"))
                                               (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] (uuid/named "prompt-31")))
                                           (uuid/named "palette-2"))]
     (is (= [(uuid/named "palette-2") C/palette-2] (subject/selected-palette app-after)))
     (is (nil? (subject/selected-prompt app-after))))))

((deftest select-prompt-test
   (let [app-after (subject/select-prompt (subject/select-palette C/default-app (uuid/named "palette-1")) (uuid/named "prompt-11"))]
     (is (= [(uuid/named "prompt-11") {::prompt/text "Comment" ::prompt/color 0}] (subject/selected-prompt app-after))))))
