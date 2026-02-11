(ns adnotare.core.event.annotate-test
  (:require
   [adnotare.core.event :as event]
   [adnotare.core.event.annotate]
   [adnotare.test.constants :refer [default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest annotate-events
  (testing "select annotation dispatches reveal and focus"
    (let [result (event/handle default-state {:event/type :annotate/select-annotation
                                              :id (uuid/named "ann-1")})]
      (is (= (uuid/named "ann-1")
             (get-in result [:state :state/ui :ui/annotate :annotate/selected-annotation-id])))
      (is (= {:event/type :annotate/reveal-document-selection} (:dispatch result)))
      (is (= {:ms 50 :event {:event/type :annotate/focus-note}} (:dispatch-later result)))))
  (testing "reveal selection emits ui updates only when selected annotation exists"
    (let [with-selection (event/handle default-state {:event/type :annotate/reveal-document-selection})
          no-selection-state (assoc-in default-state [:state/ui :ui/annotate :annotate/selected-annotation-id] nil)
          without-selection (event/handle no-selection-state {:event/type :annotate/reveal-document-selection})]
      (is (= :reveal-range (get-in with-selection [:ui :updates 0 :op])))
      (is (= {} (dissoc without-selection :state)))))
  (testing "add annotation requests document selection"
    (let [result (event/handle default-state {:event/type :annotate/add-annotation
                                              :prompt-id (uuid/named "default-prompt-1")})]
      (is (= :annotate/doc (get-in result [:get-selection :node-key])))
      (is (= {:event/type :annotate/add-annotation-on-selection
              :prompt-id (uuid/named "default-prompt-1")}
             (get-in result [:get-selection :on-selection])))))
  (testing "delete annotation removes record, clears selection, and can consume fx event"
    (let [fx-event {:kind :mouse}
          result (event/handle default-state {:event/type :annotate/delete-annotation
                                              :id (uuid/named "ann-2")
                                              :fx/event fx-event})]
      (is (nil? (get-in result [:state :state/document :document/annotations :by-id (uuid/named "ann-2")])))
      (is (nil? (get-in result [:state :state/ui :ui/annotate :annotate/selected-annotation-id])))
      (is (= fx-event (:consume-event result)))))
  (testing "add annotation on selection: empty selection warns"
    (let [result (event/handle default-state {:event/type :annotate/add-annotation-on-selection
                                              :prompt-id (uuid/named "default-prompt-1")
                                              :selection nil})]
      (is (= :warning (get-in result [:toast :toast/type])))))
  (testing "add annotation on selection: missing active palette warns"
    (let [no-palette-state (assoc-in default-state [:state/ui :ui/annotate :annotate/active-palette-id] nil)
          result (event/handle no-palette-state {:event/type :annotate/add-annotation-on-selection
                                                 :prompt-id (uuid/named "default-prompt-1")
                                                 :selection {:start 0 :end 4 :text "Test"}})]
      (is (= :warning (get-in result [:toast :toast/type])))))
  (testing "add annotation on selection: creates annotation and focuses note"
    (let [before-count (count (get-in default-state [:state/document :document/annotations :by-id]))
          result (event/handle default-state {:event/type :annotate/add-annotation-on-selection
                                              :prompt-id (uuid/named "default-prompt-1")
                                              :selection {:start 0 :end 4 :text "Test"}})
          annotations (get-in result [:state :state/document :document/annotations :by-id])
          after-count (count annotations)
          selected-id (get-in result [:state :state/ui :ui/annotate :annotate/selected-annotation-id])]
      (is (= (inc before-count) after-count))
      (is (contains? annotations selected-id))
      (is (= {:event/type :annotate/clear-document-selection} (:dispatch result)))
      (is (= {:ms 50 :event {:event/type :annotate/focus-note}} (:dispatch-later result)))))
  (testing "clear document selection and focus note emit ui ops"
    (is (= :clear-selection
           (get-in (event/handle default-state {:event/type :annotate/clear-document-selection})
                   [:ui :updates 0 :op])))
    (is (= :focus
           (get-in (event/handle default-state {:event/type :annotate/focus-note})
                   [:ui :updates 0 :op]))))
  (testing "paste doc requests clipboard"
    (is (= {:on-clipboard {:event/type :annotate/paste-doc-on-clipboard}}
           (:get-clipboard (event/handle default-state {:event/type :annotate/paste-doc})))))
  (testing "paste doc on clipboard branches"
    (let [empty-result (event/handle default-state {:event/type :annotate/paste-doc-on-clipboard
                                                     :text ""})
          with-annotations (event/handle default-state {:event/type :annotate/paste-doc-on-clipboard
                                                        :text "new text"})
          without-annotations-state (assoc-in default-state [:state/document :document/annotations :by-id] {})
          without-annotations (event/handle without-annotations-state {:event/type :annotate/paste-doc-on-clipboard
                                                                       :text "new text"})]
      (is (= :warning (get-in empty-result [:toast :toast/type])))
      (is (= {:event/type :annotate/replace-doc :text "new text"}
             (get-in with-annotations [:confirm :yes-event])))
      (is (= {:event/type :annotate/replace-doc :text "new text"}
             (:dispatch without-annotations)))))
  (testing "replace doc clears annotations and selected annotation"
    (let [result (event/handle default-state {:event/type :annotate/replace-doc :text "new"})]
      (is (= "new" (get-in result [:state :state/document :document/text])))
      (is (= {} (get-in result [:state :state/document :document/annotations :by-id])))
      (is (nil? (get-in result [:state :state/ui :ui/annotate :annotate/selected-annotation-id])))))
  (testing "update selected annotation note updates only when selected id exists"
    (let [updated (event/handle default-state {:event/type :annotate/update-selected-annotation-note
                                               :text "New note"})
          no-selection-state (assoc-in default-state [:state/ui :ui/annotate :annotate/selected-annotation-id] nil)
          unchanged (event/handle no-selection-state {:event/type :annotate/update-selected-annotation-note
                                                      :text "Ignored"})]
      (is (= "New note"
             (get-in updated [:state :state/document :document/annotations :by-id (uuid/named "ann-2") :annotation/note])))
      (is (= no-selection-state (:state unchanged)))))
  (testing "switch palette updates active palette and persists"
    (let [palette-id (uuid/named "default-palette")
          result (event/handle default-state {:event/type :annotate/switch-palette
                                              :palette-id palette-id})]
      (is (= palette-id (get-in result [:state :state/ui :ui/annotate :annotate/active-palette-id])))
      (is (= (:state/palettes (:state result))
             (get-in result [:persist-palettes :palettes])))))
  (testing "copy annotations branches"
    (let [copied (event/handle default-state {:event/type :annotate/copy-annotations})
          no-annotations-state (assoc-in default-state [:state/document :document/annotations :by-id] {})
          warned (event/handle no-annotations-state {:event/type :annotate/copy-annotations})]
      (is (string? (get-in copied [:copy-to-clipboard :text])))
      (is (= :success (get-in copied [:toast :toast/type])))
      (is (= :warning (get-in warned [:toast :toast/type]))))))
