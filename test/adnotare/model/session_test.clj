(ns adnotare.model.session-test
  (:require [adnotare.model.session :as session]
            [adnotare.test.constants :refer [default-session]]
            [adnotare.util.uuid :as uuid]
            [clojure.test :refer [deftest testing is]]))

(defn- sort-by-start [annotations]
  (vec (sort-by (comp :start :selection) annotations)))

(deftest doc-rich-text
  (testing "builds rich text model from document text and annotations"
    (let [model (session/doc-rich-text default-session)]
      (is (= {:text "Hello World! This is a test of Adnotare."
              :spans [{:start 13 :end 27 :style-classes ["rich-text-span" "color-5"]}
                      {:start 31 :end 39 :style-classes ["rich-text-span" "color-3" "selected"]}]}
             model)))))

(deftest annotation-ids
  (testing "returns annotation ids"
    (is (= #{(uuid/named "ann-1") (uuid/named "ann-2")}
           (set (session/annotation-ids default-session))))))

(deftest active-palette
  (testing "returns active palette data when active"
    (let [palette (session/active-palette default-session)]
      (is (= "Default" (:label palette)))
      (is (= ["Generic" "Are you sure about this?"] (take 2 (map :text (:prompts palette)))))))
  ;; TODO: Use public function (delete-all-palettes).
  (testing "returns nil when no active palette"
    (is (nil? (session/active-palette (assoc-in default-session [:annotate :active-palette-id] nil))))))

(deftest palette-options
  (testing "returns palette options sorted case-insensitively by label"
    (let [session-with-palettes (assoc default-session
                                       :palettes
                                       {:by-id {(uuid/named "palette-z") {:label "zeta" :prompts {:by-id {} :order []}}
                                                (uuid/named "palette-a") {:label "Alpha" :prompts {:by-id {} :order []}}
                                                (uuid/named "palette-b") {:label "beta" :prompts {:by-id {} :order []}}}
                                        :last-used-ms {}})]
      (is (= [{:id (uuid/named "palette-a") :label "Alpha"}
              {:id (uuid/named "palette-b") :label "beta"}
              {:id (uuid/named "palette-z") :label "zeta"}]
             (session/palette-options session-with-palettes))))))

(deftest annotations
  (testing "denormalizes annotations with prompt and selection state"
    (let [annotations (sort-by-start (session/annotations default-session))]
      (is (= [{:id (uuid/named "ann-1")
               :note ""
               :prompt {:text "Explain this" :color 5}
               :prompt-ref {:palette-id (uuid/named "default-palette") :prompt-id (uuid/named "default-prompt-2")}
               :selected? false
               :selection {:start 13 :end 27 :text "This is a test"}}
              {:id (uuid/named "ann-2")
               :note "Etymology"
               :prompt {:text "Give more details" :color 3}
               :prompt-ref {:palette-id (uuid/named "default-palette") :prompt-id (uuid/named "default-prompt-4")}
               :selected? true
               :selection {:start 31 :end 39 :text "Adnotare"}}]
             annotations)))))

(deftest selected-annotation
  (testing "returns selected denormalized annotation"
    (is (= {:prompt-ref {:palette-id (uuid/named "default-palette") :prompt-id (uuid/named "default-prompt-4")}
            :prompt {:text "Give more details" :color 3}
            :selected? true
            :id (uuid/named "ann-2")
            :selection {:start 31 :end 39 :text "Adnotare"}
            :note "Etymology"}
           (session/selected-annotation default-session))))
  (testing "returns nil when nothing selected"
    (is (nil? (session/selected-annotation
               (session/clear-annotation-selection default-session))))))

(deftest annotations-for-llm
  (testing "formats annotations for an LLM"
    (let [annotations (session/annotations-for-llm default-session)
          expected "<annotation>
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
      (is (= expected annotations)))))

(deftest activate-last-used-palette
  (testing "activates the most recently used palette when available"
    (let [session-with-last-used (assoc-in default-session
                                           [:palettes :last-used-ms]
                                           {(uuid/named "default-palette") 123
                                            (uuid/named "older-palette") 12})
          new-session (session/activate-last-used-palette session-with-last-used)]
      (is (= "Default" (:label (session/active-palette new-session))))))
  (testing "falls back to first palette when none used"
    (let [new-session (session/activate-last-used-palette
                       (assoc-in default-session [:annotate :active-palette-id] nil))]
      (is (= "Default" (:label (session/active-palette new-session)))))))

(deftest select-annotation
  (testing "selects the annotation with the given id"
    (let [new-session (session/select-annotation default-session (uuid/named "ann-1"))]
      (is (= (uuid/named "ann-1") (get-in new-session [:annotate :annotations :selected-id]))))))

(deftest add-annotation
  (testing "adds a new annotation and selects it"
    (let [selection {:start 6 :end 11 :text "World"}
          new-session (session/add-annotation default-session (uuid/named "default-prompt-3") selection #(uuid/named "ann-3"))]
      (is (= #{(uuid/named "ann-1") (uuid/named "ann-2") (uuid/named "ann-3")}
             (set (keys (get-in new-session [:annotate :annotations :by-id])))))
      (is (= {:prompt-ref {:palette-id (uuid/named "default-palette") :prompt-id (uuid/named "default-prompt-3")}
              :selection selection
              :note ""}
             (get-in new-session [:annotate :annotations :by-id (uuid/named "ann-3")])))
      (is (= (uuid/named "ann-3") (get-in new-session [:annotate :annotations :selected-id]))))))

(deftest update-selected-annotation-note
  (testing "replaces the note of the selected annotation"
    (let [text "New note"
          new-session (session/update-selected-annotation-note default-session text)]
      (is (= text (get-in new-session [:annotate :annotations :by-id (uuid/named "ann-2") :note]))))))

(deftest clear-annotation-selection
  (testing "clears the selected annotation"
    (is (nil? (get-in (session/clear-annotation-selection default-session)
                      [:annotate :annotations :selected-id])))))

(deftest delete-annotation
  (testing "deletes the annotation with the given id"
    (let [new-session (session/delete-annotation default-session (uuid/named "ann-1"))]
      (is (= #{(uuid/named "ann-2")}
             (set (keys (get-in new-session [:annotate :annotations :by-id])))))
      (is (= (uuid/named "ann-2") (get-in new-session [:annotate :annotations :selected-id])))))
  (testing "when the annotation is selected, clears selection"
    (let [new-session (session/delete-annotation default-session (uuid/named "ann-2"))]
      (is (= #{(uuid/named "ann-1")}
             (set (keys (get-in new-session [:annotate :annotations :by-id])))))
      (is (nil? (get-in new-session [:annotate :annotations :selected-id]))))))

(deftest replace-doc
  (testing "replaces document text and removes all annotations"
    (let [text "New document"
          new-session (session/replace-doc default-session text)]
      (is (= text (:text (session/doc-rich-text new-session))))
      (is (nil? (session/annotation-ids new-session)))
      (is (nil? (session/selected-annotation new-session))))))

(deftest set-active-palette
  (testing "sets active palette id and updates last-used timestamp"
    (let [palette-id (uuid/named "default-palette")
          now-ms 123456
          updated-session (session/set-active-palette default-session palette-id now-ms)]
      (is (= palette-id (get-in updated-session [:annotate :active-palette-id])))
      (is (= now-ms (get-in updated-session [:palettes :last-used-ms palette-id]))))))
