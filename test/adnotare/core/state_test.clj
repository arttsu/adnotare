(ns adnotare.core.state-test
  (:require
   [adnotare.core.state :as state]
   [adnotare.core.state.document :as state.document]
   [adnotare.core.state.palettes :as state.palettes]
   [adnotare.core.state.ui :as state.ui]
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [adnotare.core.state.ui.manage-prompts :as state.ui.manage-prompts]
   [adnotare.test.constants :refer [default-prompt-palette default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest document-state
  (testing "document accessors"
    (is (= (:state/document default-state) (state.document/document default-state)))
    (is (= "Hello World! This is a test of Adnotare." (state.document/text default-state)))
    (is (= (get-in default-state [:state/document :document/annotations])
           (state.document/annotations default-state)))
    (is (= (get-in default-state [:state/document :document/annotations :by-id (uuid/named "ann-1")])
           (state.document/annotation-by-id default-state (uuid/named "ann-1"))))
    (is (= #{(uuid/named "ann-1") (uuid/named "ann-2")}
           (set (state.document/annotation-ids default-state)))))
  (testing "replace-text clears annotations"
    (let [next-state (state.document/replace-text default-state "new doc")]
      (is (= "new doc" (state.document/text next-state)))
      (is (= {} (get-in next-state [:state/document :document/annotations :by-id])))))
  (testing "annotation writes and deletes"
    (let [annotation-id (uuid/named "ann-added")
          selection {:selection/start 0 :selection/end 5 :selection/text "Hello"}
          prompt-ref {:prompt-ref/palette-id (uuid/named "default-palette")
                      :prompt-ref/prompt-id (uuid/named "default-prompt-1")}
          next-state (state.document/add-annotation default-state annotation-id prompt-ref selection)
          noted-state (state.document/update-annotation-note next-state annotation-id "note")
          deleted-state (state.document/delete-annotation noted-state annotation-id)]
      (is (= {:annotation/prompt-ref prompt-ref
              :annotation/selection selection
              :annotation/note ""}
             (state.document/annotation-by-id next-state annotation-id)))
      (is (= "note"
             (get-in noted-state [:state/document :document/annotations :by-id annotation-id :annotation/note])))
      (is (nil? (state.document/annotation-by-id deleted-state annotation-id))))))

(deftest palettes-state
  (let [palette-a (uuid/named "palette-a")
        palette-b (uuid/named "palette-b")
        palettes {:by-id {palette-a {:palette/label "zeta"
                                              :palette/prompts {:by-id {} :order []}}
                                   palette-b {:palette/label "Alpha"
                                              :palette/prompts {:by-id {} :order []}}}
                  :last-used-ms {}}]
    (testing "palette accessors"
      (is (= (:state/palettes default-state) (state.palettes/palettes default-state)))
      (is (= palettes
             (:state/palettes (state.palettes/put-palettes default-state palettes))))
      (is (= (:by-id palettes)
             (state.palettes/by-id (state.palettes/put-palettes default-state palettes))))
      (is (= {:palette/label "Alpha" :palette/prompts {:by-id {} :order []}}
             (state.palettes/palette-by-id (state.palettes/put-palettes default-state palettes) palette-b))))
    (testing "ordering and derived ids"
      (let [with-palettes (state.palettes/put-palettes default-state palettes)]
        (is (= [palette-b palette-a] (vec (state.palettes/palette-id-seq with-palettes))))
        (is (= palette-b (state.palettes/first-palette-id with-palettes)))
        (is (nil? (state.palettes/most-recent-id with-palettes)))))
    (testing "mark-last-used arities and most-recent"
      (let [with-palettes (state.palettes/put-palettes default-state palettes)
            marked-a (state.palettes/mark-last-used with-palettes palette-a 10)
            marked-b (state.palettes/mark-last-used marked-a palette-b 11)
            marked-now (state.palettes/mark-last-used with-palettes palette-a)]
        (is (= 10 (get-in marked-a [:state/palettes :last-used-ms palette-a])))
        (is (= palette-b (state.palettes/most-recent-id marked-b)))
        (is (integer? (get-in marked-now [:state/palettes :last-used-ms palette-a])))))))

(deftest ui-state
  (testing "route and initialized flags"
    (let [next-state (-> default-state
                         (state.ui/set-route :manage-prompts)
                         (state.ui/set-initialized false))]
      (is (= (:state/ui default-state) (state.ui/ui default-state)))
      (is (= :manage-prompts (state.ui/route next-state)))
      (is (false? (state.ui/initialized? next-state)))))
  (testing "toasts and sorting"
    (let [toast-a {:toast/text "A" :toast/type :info :toast/duration-ms 100 :toast/created-at-ms 2}
          toast-b {:toast/text "B" :toast/type :success :toast/duration-ms 100 :toast/created-at-ms 1}
          state-with-toasts (-> default-state
                                (state.ui/add-toast (uuid/named "toast-a") toast-a)
                                (state.ui/add-toast (uuid/named "toast-b") toast-b))
          toast-items (state.ui/toasts state-with-toasts)]
      (is (= [(uuid/named "toast-b") (uuid/named "toast-a")]
             (mapv :toast/id toast-items)))
      (is (= ["B" "A"] (mapv :toast/text toast-items)))
      (is (nil? (get-in (state.ui/clear-toast state-with-toasts (uuid/named "toast-a"))
                        [:state/ui :ui/toasts :by-id (uuid/named "toast-a")])))))
  (testing "toast factory"
    (let [toast-default (state.ui/->toast "Hello" :info)
          toast-custom (state.ui/->toast "World" :success 42)]
      (is (= "Hello" (:toast/text toast-default)))
      (is (= 1500 (:toast/duration-ms toast-default)))
      (is (= 42 (:toast/duration-ms toast-custom)))
      (is (integer? (:toast/created-at-ms toast-custom))))))

(deftest annotate-ui-state
  (testing "annotate ui selectors and mutations"
    (let [state-1 (state.ui.annotate/set-active-palette default-state (uuid/named "default-palette"))
          state-2 (state.ui.annotate/select-annotation state-1 (uuid/named "ann-1"))
          state-3 (state.ui.annotate/clear-annotation-selection state-2)]
      (is (= (get-in default-state [:state/ui :ui/annotate])
             (state.ui.annotate/annotate-ui default-state)))
      (is (= (uuid/named "default-palette") (state.ui.annotate/active-palette-id state-1)))
      (is (= (uuid/named "ann-1") (state.ui.annotate/selected-annotation-id state-2)))
      (is (nil? (state.ui.annotate/selected-annotation-id state-3))))))

(deftest manage-prompts-ui-state
  (testing "selectors and synchronization"
    (let [state-1 (state.ui.manage-prompts/select-palette default-state (uuid/named "default-palette"))
          state-2 (state.ui.manage-prompts/select-prompt state-1 (uuid/named "default-prompt-4"))
          state-3 (state.ui.manage-prompts/sync-with-active-palette state-2)]
      (is (= (get-in default-state [:state/ui :ui/manage-prompts])
             (state.ui.manage-prompts/manage-prompts-ui default-state)))
      (is (= (uuid/named "default-palette")
             (state.ui.manage-prompts/selected-palette-id state-1)))
      (is (= (uuid/named "default-prompt-4")
             (state.ui.manage-prompts/selected-prompt-id state-2)))
      (is (= (uuid/named "default-palette")
             (state.ui.manage-prompts/selected-palette-id state-3)))
      (is (nil? (state.ui.manage-prompts/selected-prompt-id state-3)))))
  (testing "selected palette defaults to active palette"
    (is (= (uuid/named "default-palette")
           (state.ui.manage-prompts/selected-palette-id default-state)))))

(deftest root-state-with-palettes
  (testing "with-palettes sets initialized and picks most-recent"
    (let [palette-id-a (uuid/named "palette-a")
          palette-id-b (uuid/named "palette-b")
          palettes {:by-id {palette-id-a default-prompt-palette
                                     palette-id-b default-prompt-palette}
                    :last-used-ms {palette-id-a 10
                                            palette-id-b 11}}
          next-state (state/initialize state/initial palettes)]
      (is (true? (get-in next-state [:state/ui :ui/initialized?])))
      (is (= palette-id-b
             (get-in next-state [:state/ui :ui/annotate :annotate/active-palette-id])))))
  (testing "with-palettes falls back to first palette when no usage timestamps"
    (let [palette-id-a (uuid/named "palette-a")
          palette-id-b (uuid/named "palette-b")
          palettes {:by-id {palette-id-a (assoc default-prompt-palette :palette/label "B")
                                     palette-id-b (assoc default-prompt-palette :palette/label "A")}
                    :last-used-ms {}}
          next-state (state/initialize state/initial palettes)]
      (is (= palette-id-b
             (get-in next-state [:state/ui :ui/annotate :annotate/active-palette-id]))))))
