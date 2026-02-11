(ns adnotare.core.derive.annotate-test
  (:require
   [adnotare.core.derive.annotate :as derive.annotate]
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [adnotare.test.constants :refer [default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest derive-annotate
  (testing "annotation resolves prompt and selection state"
    (let [projection (derive.annotate/annotation default-state (uuid/named "ann-2"))]
      (is (= (uuid/named "ann-2") (:annotation/id projection)))
      (is (= "Give more details" (get-in projection [:annotation/prompt :prompt/text])))
      (is (true? (:annotation/selected? projection)))))
  (testing "annotation returns nil for unknown id"
    (is (nil? (derive.annotate/annotation default-state (uuid/named "missing-ann")))))
  (testing "annotations are sorted by selection start"
    (is (= [(uuid/named "ann-1") (uuid/named "ann-2")]
           (mapv :annotation/id (derive.annotate/annotations default-state)))))
  (testing "selected annotation follows ui selected id"
    (is (= (uuid/named "ann-2")
           (:annotation/id (derive.annotate/selected-annotation default-state))))
    (is (nil? (derive.annotate/selected-annotation
               (state.ui.annotate/clear-annotation-selection default-state)))))
  (testing "doc-rich-text includes style classes and selected marker"
    (is (= {:rich-text/text "Hello World! This is a test of Adnotare."
            :rich-text/spans [{:span/start 13 :span/end 27 :span/style-classes ["rich-text-span" "color-5"]}
                              {:span/start 31 :span/end 39 :span/style-classes ["rich-text-span" "color-3" "selected"]}]}
           (derive.annotate/doc-rich-text default-state))))
  (testing "doc-rich-text falls back to color-0 when prompt not found"
    (let [state (assoc-in default-state
                          [:state/document :document/annotations :by-id (uuid/named "ann-1") :annotation/prompt-ref :prompt-ref/prompt-id]
                          (uuid/named "missing-prompt"))]
      (is (= ["rich-text-span" "color-0"]
             (-> state derive.annotate/doc-rich-text :rich-text/spans first :span/style-classes)))))
  (testing "annotations-str formats payload and omits blank note sections"
    (let [payload (derive.annotate/annotations-str default-state)]
      (is (.contains payload "<quote>\nThis is a test\n</quote>"))
      (is (.contains payload "<prompt>\nExplain this\n</prompt>"))
      (is (.contains payload "<note>\nEtymology\n</note>"))
      (is (not (.contains payload "<note>\n\n</note>"))))))
