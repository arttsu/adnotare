(ns adnotare.core.event.manage-prompts-test
  (:require
   [adnotare.core.event :as event]
   [adnotare.core.event.manage-prompts]
   [adnotare.test.constants :refer [default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest manage-prompts-events
  (testing "select palette and prompt mutate ui selections"
    (let [palette-id (uuid/named "default-palette")
          prompt-id (uuid/named "default-prompt-4")
          selected-palette (event/handle default-state {:event/type :manage-prompts/select-palette
                                                        :palette-id palette-id})
          selected-prompt (event/handle (:state selected-palette) {:event/type :manage-prompts/select-prompt
                                                                   :prompt-id prompt-id})]
      (is (= palette-id
             (get-in selected-palette [:state :state/ui :ui/manage-prompts :manage-prompts/selected-palette-id])))
      (is (= prompt-id
             (get-in selected-prompt [:state :state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id]))))))
