(ns adnotare.core.derive-test
  (:require
   [adnotare.core.derive.annotate :as derive.annotate]
   [adnotare.core.derive.palettes :as derive.palettes]
   [adnotare.core.state.ui.manage-prompts :as ui.manage-prompts]
   [adnotare.test.constants :refer [default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest palette-projections
  (testing "palette options are sorted by label"
    (let [state (assoc default-state
                       :state/palettes
                       {:palettes/by-id {(uuid/named "palette-z") {:palette/label "zeta" :palette/prompts {:by-id {} :order []}}
                                         (uuid/named "palette-a") {:palette/label "Alpha" :palette/prompts {:by-id {} :order []}}
                                         (uuid/named "palette-b") {:palette/label "beta" :palette/prompts {:by-id {} :order []}}}
                        :palettes/order [(uuid/named "palette-z") (uuid/named "palette-a") (uuid/named "palette-b")]
                        :palettes/last-used-ms {}})]
      (is (= [{:option/id (uuid/named "palette-a") :option/label "Alpha"}
              {:option/id (uuid/named "palette-b") :option/label "beta"}
              {:option/id (uuid/named "palette-z") :option/label "zeta"}]
             (derive.palettes/palette-options state)))))
  (testing "manage-prompts selected prompt resolves from derived palette"
    (let [state (-> default-state
                    (ui.manage-prompts/select-palette (uuid/named "default-palette"))
                    (ui.manage-prompts/select-prompt (uuid/named "default-prompt-4")))]
      (is (= "Give more details"
             (some-> (derive.palettes/manage-prompts-selected-prompt state) :prompt/text))))))

(deftest annotation-projections
  (testing "annotation projection resolves prompt and selected state"
    (let [annotation (derive.annotate/annotation default-state (uuid/named "ann-2"))]
      (is (= true (:annotation/selected? annotation)))
      (is (= "Give more details" (get-in annotation [:annotation/prompt :prompt/text])))))
  (testing "annotations are sorted by start position"
    (is (= [(uuid/named "ann-1") (uuid/named "ann-2")]
           (mapv :annotation/id (derive.annotate/annotations default-state)))))
  (testing "doc-rich-text includes color classes and selected marker"
    (is (= {:rich-text/text "Hello World! This is a test of Adnotare."
            :rich-text/spans [{:span/start 13 :span/end 27 :span/style-classes ["rich-text-span" "color-5"]}
                              {:span/start 31 :span/end 39 :span/style-classes ["rich-text-span" "color-3" "selected"]}]}
           (derive.annotate/doc-rich-text default-state))))
  (testing "annotations-str formats clipboard payload"
    (let [expected "<annotation>
<quote>
This is a test
</quote>
<prompt>
Explain this
</prompt>
</annotation>

<annotation>
<quote>
Adnotare
</quote>
<prompt>
Give more details
</prompt>
<note>
Etymology
</note>
</annotation>
"]
      (is (= expected (derive.annotate/annotations-str default-state))))))
