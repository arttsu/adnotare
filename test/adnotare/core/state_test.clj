(ns adnotare.core.state-test
  (:require
   [adnotare.core.state.document :as state.document]
   [adnotare.core.state.palettes :as state.palettes]
   [adnotare.core.state.ui :as state.ui]
   [adnotare.core.state.ui.manage-prompts :as ui.manage-prompts]
   [adnotare.test.constants :refer [default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest document-state-mutations
  (testing "replace-text clears annotations"
    (let [state (state.document/replace-text default-state "new document")]
      (is (= "new document" (get-in state [:state/document :document/text])))
      (is (empty? (get-in state [:state/document :document/annotations :by-id])))))
  (testing "add-annotation ignores nil ids"
    (let [selection {:selection/start 0 :selection/end 4 :selection/text "Test"}
          bad-state (state.document/add-annotation default-state
                                                   nil
                                                   {:prompt-ref/palette-id nil
                                                    :prompt-ref/prompt-id (uuid/named "default-prompt-1")}
                                                   selection)]
      (is (= default-state bad-state))))
  (testing "add-annotation writes normalized record"
    (let [selection {:selection/start 0 :selection/end 5 :selection/text "Hello"}
          state (state.document/add-annotation default-state
                                               (uuid/named "ann-3")
                                               {:prompt-ref/palette-id (uuid/named "default-palette")
                                                :prompt-ref/prompt-id (uuid/named "default-prompt-1")}
                                               selection)]
      (is (= {:annotation/prompt-ref {:prompt-ref/palette-id (uuid/named "default-palette")
                                      :prompt-ref/prompt-id (uuid/named "default-prompt-1")}
              :annotation/selection selection
              :annotation/note ""}
             (get-in state [:state/document :document/annotations :by-id (uuid/named "ann-3")])))))
  (testing "delete-annotation removes id from by-id"
    (let [state (state.document/delete-annotation default-state (uuid/named "ann-2"))]
      (is (nil? (get-in state [:state/document :document/annotations :by-id (uuid/named "ann-2")])))))
  (testing "update-annotation-note updates annotation by id"
    (let [state (state.document/update-annotation-note default-state (uuid/named "ann-2") "New note")]
      (is (= "New note"
             (get-in state [:state/document :document/annotations :by-id (uuid/named "ann-2") :annotation/note]))))))

(deftest ui-state-mutations
  (testing "toast add and clear"
    (let [toast-id (uuid/named "toast-1")
          toast {:toast/text "hello" :toast/type :info :toast/duration-ms 10 :toast/created-at-ms 1}
          state (-> default-state
                    (state.ui/add-toast toast-id toast)
                    (state.ui/clear-toast toast-id))]
      (is (empty? (get-in state [:state/ui :ui/toasts :by-id])))))
  (testing "manage prompts selectors"
    (let [state (-> default-state
                    (ui.manage-prompts/select-palette (uuid/named "default-palette"))
                    (ui.manage-prompts/select-prompt (uuid/named "default-prompt-4")))]
      (is (= (uuid/named "default-palette")
             (ui.manage-prompts/selected-palette-id state)))
      (is (= (uuid/named "default-prompt-4")
             (ui.manage-prompts/selected-prompt-id state)))))
  (testing "sync with active palette clears selected prompt"
    (let [state (-> default-state
                    (ui.manage-prompts/select-palette (uuid/named "other-palette"))
                    (ui.manage-prompts/select-prompt (uuid/named "default-prompt-1"))
                    (ui.manage-prompts/sync-with-active-palette))]
      (is (= (uuid/named "default-palette")
             (ui.manage-prompts/selected-palette-id state)))
      (is (nil? (ui.manage-prompts/selected-prompt-id state))))))

(deftest palettes-state-mutations
  (testing "mark-last-used ignores nil palette id"
    (is (= default-state
           (state.palettes/mark-last-used default-state nil 10))))
  (testing "mark-last-used updates timestamp and most-recent-id"
    (let [state (state.palettes/mark-last-used default-state (uuid/named "default-palette") 123)]
      (is (= 123
             (get-in state [:state/palettes :palettes/last-used-ms (uuid/named "default-palette")])))
      (is (= (uuid/named "default-palette")
             (state.palettes/most-recent-id state))))))
