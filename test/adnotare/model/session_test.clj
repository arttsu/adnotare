(ns adnotare.model.session-test
  (:require [adnotare.model.schema :as S]
            [adnotare.model.session :as session]
            [adnotare.test.constants :refer [default-state]]
            [adnotare.test.schema :refer [is-valid]]
            [adnotare.util.uuid :refer [uuid]]
            [clojure.test :refer [deftest testing is]]))

(deftest doc-rich-text
  (testing "builds rich text model from doc and annotations"
    (let [model (session/doc-rich-text default-state)]
      (is-valid S/RichTextModel model)
      (is (= {:text "Hello World! This is a test of Adnotare."
              :spans [{:start 13 :end 27 :style-classes ["rich-text-span" "color-5"]}
                      {:start 31 :end 39 :style-classes ["rich-text-span" "color-3" "selected"]}]}
             model)))))

(deftest selected-annotation-note
  (testing "returns the note of the selected annotation"
    (is (= "Etymology" (session/selected-annotation-note default-state))))
  (testing "returns nil when no annotation is selected"
    (let [state (session/clear-annotation-selection default-state)]
      (is (nil? (session/selected-annotation-note state))))))

(deftest annotations-str
  (testing "formats annotations for an LLM"
    (let [annotations (session/annotations-str default-state)]
      (is (= (str "<annotation>\n"
                  "  <quote>\n"
                  "    This is a test\n"
                  "  </quote>\n"
                  "  <prompt>\n"
                  "    Explain this\n"
                  "  </prompt>\n"
                  "</annotation>\n\n"
                  "<annotation>\n"
                  "  <quote>\n"
                  "    Adnotare\n"
                  "  </quote>\n"
                  "  <prompt>\n"
                  "    Give more details\n"
                  "  </prompt>\n"
                  "  <note>\n"
                  "    Etymology\n"
                  "  </note>\n"
                  "</annotation>\n")
             annotations)))))

(deftest select-annotation
  (testing "selects the annotation with the given ID"
    (let [new-state (session/select-annotation default-state (uuid "ann-1"))]
      (is-valid S/State new-state)
      (is (= (uuid "ann-1") (session/selected-annotation-id new-state))))))

(deftest add-annotation
  (testing "adds a new annotation and selects it"
    (let [prompt-ref {:palette-id (uuid "default-palette") :prompt-id (uuid "default-prompt-3")}
          selection {:start 6 :end 11 :text "World"}
          new-state (session/add-annotation default-state prompt-ref selection (partial uuid "ann-3"))]
      (is-valid S/State new-state)
      (is (= #{(uuid "ann-1") (uuid "ann-2") (uuid "ann-3")} (set (keys (session/annotations-by-id new-state)))))
      (is (= {:prompt-ref prompt-ref :selection selection :note ""} (session/annotation-by-id new-state (uuid "ann-3"))))
      (is (= (uuid "ann-3") (session/selected-annotation-id new-state))))))

(deftest delete-annotation
  (testing "deletes the annotation with the given ID"
    (let [new-state (session/delete-annotation default-state (uuid "ann-1"))]
      (is-valid S/State new-state)
      (is (= #{(uuid "ann-2")} (set (keys (session/annotations-by-id new-state)))))
      (is (= (uuid "ann-2") (session/selected-annotation-id new-state)))))
  (testing "when the annotation is selected, clears selection"
    (let [new-state (session/delete-annotation default-state (uuid "ann-2"))]
      (is-valid S/State new-state)
      (is (= #{(uuid "ann-1")} (set (keys (session/annotations-by-id new-state)))))
      (is (nil? (session/selected-annotation-id new-state))))))

(deftest replace-doc
  (testing "replaces document text and removes all annotations"
    (let [text "New document"
          new-state (session/replace-doc default-state text)]
      (is-valid S/State new-state)
      (is (= text (session/doc-text new-state)))
      (is (empty? (session/annotations-by-id new-state)))
      (is (nil? (session/selected-annotation-id new-state))))))

(deftest update-selected-annotation-note
  (testing "replaces the note of the selected annotation"
    (let [text "New note"
          new-state (session/update-selected-annotation-note default-state text)]
      (is-valid S/State new-state)
      (is (= text (session/selected-annotation-note new-state))))))
