(ns adnotare.core.features.annotate-test
  (:require
   [adnotare.core.features.annotate :as subject]
   [adnotare.core.model.annotation :as annotation]
   [adnotare.core.model.annotator :as annotator]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.document :as document]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.palettes :as palettes]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.model.selection :as selection]
   [adnotare.core.util.uuid :as uuid]
   [adnotare.test.core.constants :as C]
   [adnotare.test.core.factory :as factory]
   [clojure.string :as string]
   [clojure.test :refer [are deftest is testing]]))

(deftest active-palette-test
  (let [[id palette] (subject/active-palette C/default-app)]
    (is (= (uuid/named "palette-1") id))
    (is (= "Palette One" (::palette/label palette)))))

(deftest document-rich-text-test
  (is (= {:text "Hello World! This is a test of Adnotare."
          :spans
          [{:start 13 :end 27 :style-class ["rich-text-span" "color-7"]}
           {:start 31 :end 39 :style-class ["rich-text-span" "color-3" "selected"]}]}
         (subject/document-rich-text C/default-app))))

(deftest annotations-test
  (let [annotations (subject/annotations C/default-app)]
    (is (= [[(uuid/named "annotation-1") "Provide evidence" "This is a test" "" false]
            [(uuid/named "annotation-2") "Explain" "Adnotare" "Etymology" true]]
           (map (fn [[id {::annotation/keys [note selected?]
                          {prompt-text ::prompt/label} ::annotation/prompt
                          {::selection/keys [quote]} ::annotation/selection}]]
                  [id prompt-text quote note selected?])
                annotations)))))

(deftest activate-initial-palette-test
  (doseq [[scenario app expected-active-palette]
          [["some used, activates the last used" (assoc-in C/default-app [::app/palettes :last-used-ms (uuid/named "palette-2")] 2000) "Palette Two"]
           ["none used, activates the first alphabetically"
            (update-in C/default-app [::app/palettes :last-used-ms] dissoc (uuid/named "palette-1"))
            "Palette One"]
           ["no palettes, does nothing" (assoc C/default-app ::app/palettes palettes/base) nil]]]
    (testing scenario
      (let [app-after (subject/activate-initial-palette app)
            [_id active-palette] (subject/active-palette app-after)]
        (is (= expected-active-palette (::palette/label active-palette)))))))

(deftest select-annotation-test
  (let [app-after (subject/select-annotation C/default-app (uuid/named "annotation-1"))]
    (is (= (uuid/named "annotation-1") (get-in app-after [::app/annotator ::annotator/selected-annotation-id])))))

(deftest delete-annotation-test
  (let [app-after (subject/delete-annotation C/default-app (uuid/named "annotation-2"))]
    (is (= [(uuid/named "annotation-1")]
           (keys (get-in app-after [::app/document ::document/annotations :by-id]))))
    (is (nil? (get-in app-after [::app/annotator ::annotator/selected-annotation-id])))))

(deftest add-annotation-test
  (let [selection (factory/->selection {:start 0 :end 5 :from (get-in C/default-app [::app/document ::document/text])})
        app-after (subject/add-annotation C/default-app (uuid/named "prompt-11") selection #(uuid/named "annotation-3"))]
    (is (= 3 (count (get-in app-after [::app/document ::document/annotations :by-id]))))
    (is (= (factory/->annotation {:palette-id (uuid/named "palette-1")
                                  :prompt-id (uuid/named "prompt-11")
                                  :start 0
                                  :end 5
                                  :quote "Hello"
                                  :note ""})
           (get-in app-after [::app/document ::document/annotations :by-id (uuid/named "annotation-3")])))
    (is (= (uuid/named "annotation-3") (get-in app-after [::app/annotator ::annotator/selected-annotation-id])))))

(deftest any-annotation-selected?-test
  (are [result app] (= result (subject/any-annotation-selected? app))
    true C/default-app
    false (assoc-in C/default-app [::app/annotator ::annotator/selected-annotation-id] nil)))

(deftest selected-annotation-note-test
  (is (= "Etymology"
         (subject/selected-annotation-note C/default-app))))

(deftest put-selected-annotation-note-test
  (let [app-after (subject/put-selected-annotation-note C/default-app "New note")]
    (is (= "New note" (get-in app-after [::app/document ::document/annotations :by-id (uuid/named "annotation-2") ::annotation/note])))))

(deftest selected-annotation-range-test
  (is (= {:start 31 :end 39}
         (subject/selected-annotation-range C/default-app))))

(deftest put-document-text-test
  (let [app-after (subject/put-document-text C/default-app "New text")]
    (is (= "New text" (get-in app-after [::app/document ::document/text])))
    (is (empty? (get-in app-after [::app/document ::document/annotations :by-id])))
    (is (nil? (get-in app-after [::app/annotator ::annotator/selected-annotation-id])))))

(deftest annotations-as-llm-prompt-test
  (is (= "<annotation>
<quote>
This is a test
</quote>
<prompt>
Provide evidence
</prompt>
</annotation>

<annotation>
<quote>
Adnotare
</quote>
<prompt>
Explain
</prompt>
<note>
Etymology
</note>
</annotation>
"
         (subject/annotations-as-llm-prompt C/default-app))))

(deftest annotations-as-llm-prompt-prefers-instructions-test
  (let [app (assoc-in C/default-app
                      [::app/palettes :by-id (uuid/named "palette-1") ::palette/prompts :by-id (uuid/named "prompt-12") ::prompt/instructions]
                      "Detailed explanation instruction")
        output (subject/annotations-as-llm-prompt app)]
    (is (string/includes? output "Detailed explanation instruction"))
    (is (not (string/includes? output "<prompt>\nExplain\n</prompt>")))))

(deftest annotations-and-text-as-llm-prompt-test
  (is (= "<document>
Hello World! This is a test of Adnotare.
</document>

<annotations>
<annotation>
<quote>
This is a test
</quote>
<prompt>
Provide evidence
</prompt>
</annotation>

<annotation>
<quote>
Adnotare
</quote>
<prompt>
Explain
</prompt>
<note>
Etymology
</note>
</annotation>
</annotations>
"
         (subject/annotations-and-document-as-llm-prompt C/default-app))))

(deftest palette-selector-options-test
  (is (= {:options [{:id (uuid/named "palette-1"), :text "Palette One"}
                    {:id (uuid/named "palette-3"), :text "Palette Three"}
                    {:id (uuid/named "palette-2"), :text "Palette Two"}]
          :selected {:id (uuid/named "palette-1"), :text "Palette One"}}
         (subject/palette-selector-options C/default-app))))

(deftest switch-palette-test
  (let [app-after (subject/switch-palette C/default-app (uuid/named "palette-2") (constantly 100500))
        [_id active-palette] (subject/active-palette app-after)]
    (is (= "Palette Two" (::palette/label active-palette)))
    (is (= 100500 (get-in app-after [::app/palettes :last-used-ms (uuid/named "palette-2")])))))

(deftest switch-palette-next-test
  (testing "moves to next palette alphabetically"
    (let [app-after (subject/switch-palette-next C/default-app)
          [active-id active-palette] (subject/active-palette app-after)]
      (is (= (uuid/named "palette-3") active-id))
      (is (= "Palette Three" (::palette/label active-palette)))
      (is (integer? (get-in app-after [::app/palettes :last-used-ms (uuid/named "palette-3")])))))
  (testing "wraps from the last palette to the first"
    (let [app (assoc-in C/default-app [::app/annotator ::annotator/active-palette-id] (uuid/named "palette-2"))
          app-after (subject/switch-palette-next app)
          [active-id active-palette] (subject/active-palette app-after)]
      (is (= (uuid/named "palette-1") active-id))
      (is (= "Palette One" (::palette/label active-palette))))))

(deftest switch-palette-prev-test
  (testing "moves to previous palette alphabetically"
    (let [app-after (subject/switch-palette-prev C/default-app)
          [active-id active-palette] (subject/active-palette app-after)]
      (is (= (uuid/named "palette-2") active-id))
      (is (= "Palette Two" (::palette/label active-palette)))
      (is (integer? (get-in app-after [::app/palettes :last-used-ms (uuid/named "palette-2")])))))
  (testing "wraps from the first palette to the last"
    (let [app-after (subject/switch-palette-prev C/default-app)
          app-after' (subject/switch-palette-prev app-after)
          [active-id active-palette] (subject/active-palette app-after')]
      (is (= (uuid/named "palette-3") active-id))
      (is (= "Palette Three" (::palette/label active-palette)))))
  (testing "no palettes leaves app unchanged"
    (let [app (assoc C/default-app
                     ::app/palettes (assoc palettes/base :last-used-ms {})
                     ::app/annotator (assoc (::app/annotator C/default-app) ::annotator/active-palette-id nil))]
      (is (= app (subject/switch-palette-prev app)))
      (is (= app (subject/switch-palette-next app))))))
