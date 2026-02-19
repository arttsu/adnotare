(ns adnotare.core.features.manage-prompts-test
  (:require
   [adnotare.core.features.manage-prompts :as subject]
   [adnotare.core.model.annotator :as annotator]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.document :as document]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.model.prompt-manager :as prompt-manager]
   [adnotare.core.util.uuid :as uuid]
   [adnotare.test.core.constants :as C]
   [clojure.test :refer [are deftest is]]))

(deftest palettes-test
  (is (= [[(uuid/named "palette-1") C/palette-1]
          [(uuid/named "palette-3") C/palette-3]
          [(uuid/named "palette-2") C/palette-2]]
         (subject/palettes C/default-app))))

(deftest selected-palette-and-prompt-test
  (are [result app] (is (= result (subject/selected-palette app)))
    nil C/default-app
    [(uuid/named "palette-3") C/palette-3] (assoc-in C/default-app
                                                     [::app/prompt-manager ::prompt-manager/selected-palette-id]
                                                     (uuid/named "palette-3")))

  (are [result app] (is (= result (subject/selected-prompt app)))
    nil C/default-app
    nil (assoc-in C/default-app
                  [::app/prompt-manager ::prompt-manager/selected-palette-id]
                  (uuid/named "palette-3"))
    [(uuid/named "prompt-31") {::prompt/label "Comment" ::prompt/instructions "" ::prompt/color 0}]
    (-> C/default-app
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] (uuid/named "palette-3"))
        (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] (uuid/named "prompt-31")))))

(deftest select-palette-and-prompt-test
  (let [app-after (subject/select-palette (-> C/default-app
                                              (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] (uuid/named "palette-3"))
                                              (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] (uuid/named "prompt-31")))
                                          (uuid/named "palette-2"))]
    (is (= [(uuid/named "palette-2") C/palette-2] (subject/selected-palette app-after)))
    (is (nil? (subject/selected-prompt app-after)))
    (is (= {::prompt-manager/errors {}}
           (get-in app-after [::app/prompt-manager ::prompt-manager/draft]))))

  (let [app-after (subject/select-prompt (subject/select-palette C/default-app (uuid/named "palette-1"))
                                         (uuid/named "prompt-11"))]
    (is (= [(uuid/named "prompt-11") {::prompt/label "Comment" ::prompt/instructions "" ::prompt/color 0}]
           (subject/selected-prompt app-after)))))

(deftest draft-validation-test
  (let [app (subject/select-prompt (subject/select-palette C/default-app (uuid/named "palette-1"))
                                   (uuid/named "prompt-11"))
        invalid (subject/update-draft-prompt-label app "")
        valid (subject/update-draft-prompt-label invalid "Renamed")]
    (is (true? (subject/invalid-draft? invalid)))
    (is (= "Label cannot be empty" (get (subject/validation-errors invalid) :prompt-label)))
    (is (= "" (subject/draft-prompt-label invalid)))

    (is (false? (subject/invalid-draft? valid)))
    (is (= "Renamed" (subject/draft-prompt-label valid)))
    (is (= "Renamed"
           (get-in valid [::app/palettes :by-id (uuid/named "palette-1") ::palette/prompts :by-id (uuid/named "prompt-11") ::prompt/label])))))

(deftest add-palette-and-prompt-transformers-test
  (let [ids (atom [(uuid/named "palette-new") (uuid/named "prompt-new") (uuid/named "prompt-added")])]
    (with-redefs [uuid/random (fn [] (let [id (first @ids)] (swap! ids rest) id))]
      (let [app1 (subject/add-palette* C/default-app)
            app2 (subject/add-prompt* app1 (uuid/named "palette-new"))]
        (is (= 1 (::app/persist-token app1)))
        (is (= 2 (::app/persist-token app2)))
        (is (= "New palette" (get-in app1 [::app/palettes :by-id (uuid/named "palette-new") ::palette/label])))
        (is (= (uuid/named "prompt-new") (get-in app1 [::app/prompt-manager ::prompt-manager/selected-prompt-id])))
        (is (= (uuid/named "prompt-added") (get-in app2 [::app/prompt-manager ::prompt-manager/selected-prompt-id])))))))

(deftest can-delete-guards-test
  (is (true? (subject/can-delete-palette? C/default-app)))
  (is (false? (subject/can-delete-prompt? C/default-app (uuid/named "palette-3"))))
  (is (true? (subject/can-delete-prompt? C/default-app (uuid/named "palette-1")))))

(deftest move-prompt-and-wrapper-test
  (let [app-after (subject/move-prompt C/default-app (uuid/named "palette-1") (uuid/named "prompt-12") :up)
        order (get-in app-after [::app/palettes :by-id (uuid/named "palette-1") ::palette/prompts :order])
        moved* (subject/move-prompt* C/default-app (uuid/named "palette-1") (uuid/named "prompt-12") :up)]
    (is (= [(uuid/named "prompt-11")
            (uuid/named "prompt-15")
            (uuid/named "prompt-12")
            (uuid/named "prompt-14")
            (uuid/named "prompt-13")]
           order))
    (is (= 1 (::app/persist-token moved*)))))

(deftest annotation-count-test
  (is (= 2 (subject/annotation-count-for-palette C/default-app (uuid/named "palette-1"))))
  (is (= 0 (subject/annotation-count-for-palette C/default-app (uuid/named "palette-2"))))
  (is (= 1 (subject/annotation-count-for-prompt C/default-app (uuid/named "palette-1") (uuid/named "prompt-12"))))
  (is (= 0 (subject/annotation-count-for-prompt C/default-app (uuid/named "palette-2") (uuid/named "prompt-21")))))

(deftest delete-prompt-star-purges-annotations-test
  (let [app-before (-> C/default-app
                       (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] (uuid/named "palette-1"))
                       (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] (uuid/named "prompt-12"))
                       (assoc-in [::app/annotator ::annotator/selected-annotation-id] (uuid/named "annotation-2")))
        app-after (subject/delete-prompt* app-before (uuid/named "palette-1") (uuid/named "prompt-12"))]
    (is (nil? (get-in app-after [::app/document ::document/annotations :by-id (uuid/named "annotation-2")])))
    (is (nil? (get-in app-after [::app/annotator ::annotator/selected-annotation-id])))
    (is (nil? (get-in app-after [::app/prompt-manager ::prompt-manager/selected-prompt-id])))
    (is (= 1 (::app/persist-token app-after)))))

(deftest delete-palette-star-purges-and-replaces-active-test
  (let [app-before (-> C/default-app
                       (assoc-in [::app/annotator ::annotator/active-palette-id] (uuid/named "palette-1"))
                       (assoc-in [::app/prompt-manager ::prompt-manager/selected-palette-id] (uuid/named "palette-1"))
                       (assoc-in [::app/prompt-manager ::prompt-manager/selected-prompt-id] (uuid/named "prompt-12"))
                       (assoc-in [::app/annotator ::annotator/selected-annotation-id] (uuid/named "annotation-2")))
        app-after (subject/delete-palette* app-before (uuid/named "palette-1"))]
    (is (nil? (get-in app-after [::app/palettes :by-id (uuid/named "palette-1")])))
    (is (nil? (get-in app-after [::app/document ::document/annotations :by-id (uuid/named "annotation-1")])))
    (is (nil? (get-in app-after [::app/document ::document/annotations :by-id (uuid/named "annotation-2")])))
    (is (nil? (get-in app-after [::app/annotator ::annotator/selected-annotation-id])))
    (is (= (uuid/named "palette-3") (get-in app-after [::app/annotator ::annotator/active-palette-id])))
    (is (= 1 (::app/persist-token app-after)))))
