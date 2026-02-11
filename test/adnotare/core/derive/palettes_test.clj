(ns adnotare.core.derive.palettes-test
  (:require
   [adnotare.core.derive.palettes :as derive.palettes]
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [adnotare.core.state.ui.manage-prompts :as state.ui.manage-prompts]
   [adnotare.test.constants :refer [default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest derive-palettes
  (testing "palette options are sorted by label"
    (let [state (assoc default-state
                       :state/palettes
                       {:palettes/by-id {(uuid/named "palette-z") {:palette/label "zeta" :palette/prompts {:by-id {} :order []}}
                                         (uuid/named "palette-a") {:palette/label "Alpha" :palette/prompts {:by-id {} :order []}}
                                         (uuid/named "palette-b") {:palette/label "beta" :palette/prompts {:by-id {} :order []}}}
                        :palettes/last-used-ms {}})]
      (is (= [{:option/id (uuid/named "palette-a") :option/label "Alpha"}
              {:option/id (uuid/named "palette-b") :option/label "beta"}
              {:option/id (uuid/named "palette-z") :option/label "zeta"}]
             (derive.palettes/palette-options state)))))
  (testing "palette projection and active palette/prompts"
    (let [palette-id (uuid/named "default-palette")
          projection (derive.palettes/palette default-state palette-id)]
      (is (= palette-id (:palette/id projection)))
      (is (= "Default" (:palette/label projection)))
      (is (= (uuid/named "default-prompt-1")
             (get-in projection [:palette/prompts 0 :prompt/id])))
      (is (= projection (derive.palettes/active-palette default-state)))
      (is (= (:palette/prompts projection) (derive.palettes/active-prompts default-state)))))
  (testing "palette returns nil when id is missing"
    (is (nil? (derive.palettes/palette default-state (uuid/named "missing-palette")))))
  (testing "active palette/prompts are nil when active id is nil"
    (let [state (state.ui.annotate/set-active-palette default-state nil)]
      (is (nil? (derive.palettes/active-palette state)))
      (is (nil? (derive.palettes/active-prompts state)))))
  (testing "manage-prompts selectors and projections"
    (let [state (-> default-state
                    (state.ui.manage-prompts/select-palette (uuid/named "default-palette"))
                    (state.ui.manage-prompts/select-prompt (uuid/named "default-prompt-4")))]
      (is (= (uuid/named "default-palette")
             (derive.palettes/manage-prompts-selected-palette-id state)))
      (is (= (uuid/named "default-prompt-4")
             (derive.palettes/manage-prompts-selected-prompt-id state)))
      (is (= "Give more details"
             (some-> (derive.palettes/manage-prompts-selected-prompt state) :prompt/text)))
      (is (= (uuid/named "default-palette")
             (some-> (derive.palettes/manage-prompts-palette state) :palette/id)))))
  (testing "manage-prompts selected prompt returns nil for unmatched prompt id"
    (let [state (-> default-state
                    (state.ui.manage-prompts/select-palette (uuid/named "default-palette"))
                    (state.ui.manage-prompts/select-prompt (uuid/named "missing-prompt")))]
      (is (nil? (derive.palettes/manage-prompts-selected-prompt state)))))
  (testing "manage-prompts selected palette id falls back to active palette"
    (is (= (uuid/named "default-palette")
           (derive.palettes/manage-prompts-selected-palette-id default-state)))))
