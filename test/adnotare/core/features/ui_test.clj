(ns adnotare.core.features.ui-test
  (:require
   [adnotare.core.features.annotate :as annotate]
   [adnotare.core.features.manage-prompts :as manage-prompts]
   [adnotare.core.features.ui :as subject]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.toast :as toast]
   [adnotare.core.util.result :as result]
   [adnotare.core.util.uuid :as uuid]
   [adnotare.test.core.constants :as C]
   [adnotare.test.core.factory :as factory]
   [clojure.test :refer [deftest is testing]]))

(deftest toasts
  (let [app (-> C/default-app
                (assoc-in [::app/toasts :by-id (uuid/named "toast-2")] (factory/->toast {:text "Something else happened!" :created-at-ms 200}))
                (assoc-in [::app/toasts :by-id (uuid/named "toast-1")] (factory/->toast {:text "Something happened!" :created-at-ms 100})))]
    (is (= [[(uuid/named "toast-1") (factory/->toast {:text "Something happened!" :created-at-ms 100})]
            [(uuid/named "toast-2") (factory/->toast {:text "Something else happened!" :created-at-ms 200})]]
           (subject/toasts app)))))

(deftest initialize-test
  (doseq [[scenario persisted-palettes expected-active-palette expected-init-errors]
          [["ok persisted palettes, activates last used"
            (result/ok (factory/->persisted-palettes 1 [[(uuid/named "palette-1") C/palette-1 1000] [(uuid/named "palette-2") C/palette-2 2000]]))
            "Palette Two"
            {}]
           ["ok persisted palettes, no last used, activates the first one alphabetically"
            (result/ok (factory/->persisted-palettes 1 [[(uuid/named "palette-1") C/palette-1 nil] [(uuid/named "palette-2") C/palette-2 nil]]))
            "Palette One"
            {}]
           ["empty persisted palettes" (result/ok (factory/->persisted-palettes 1 [])) nil {}]
           ["not found persisted palettes" (result/error :not-found) "Default" {::app/read-palettes :not-found}]
           ["corrupted persisted palettes" (result/error :eof) "Default" {::app/read-palettes :eof}]
           ["unsupported persisted palettes version"
            (result/ok (factory/->persisted-palettes nil [[(uuid/named "palette-1") C/palette-1 nil]]))
            "Default"
            {::app/read-palettes :unsupported-version}]
           ["invalid persisted palettes schema"
            (result/ok {:version 1 :data "bad"})
            "Default"
            {::app/read-palettes :invalid-schema}]]]
    (testing scenario
      (let [initialized-app (subject/initialize app/base persisted-palettes)
            [_ active-palette] (annotate/active-palette initialized-app)]
        (is (= true (::app/initialized? initialized-app)))
        (is (= expected-active-palette (::palette/label active-palette)))
        (is (= expected-init-errors (::app/init-errors initialized-app)))))))

(deftest add-toast-test
  (let [app-after (subject/add-toast C/default-app (uuid/named "toast-1") (factory/->toast {:text "Something happened!"}))]
    (is (= {(uuid/named "toast-1") {::toast/type ::toast/success
                                    ::toast/text "Something happened!"
                                    ::toast/created-at-ms 0}}
           (get-in app-after [::app/toasts :by-id])))))

(deftest clear-toast-test
  (let [app-before (assoc-in C/default-app [::app/toasts :by-id (uuid/named "toast-1")] (factory/->toast {:text "Something happened!"}))
        app-after (subject/clear-toast app-before (uuid/named "toast-1"))]
    (is (= {} (get-in app-after [::app/toasts :by-id])))))

((deftest goto-prompt-manager-test
   (let [app-after (subject/goto-prompt-manager C/default-app)]
     (is (= ::app/prompt-manager (::app/route app-after)))
     (is (= (uuid/named "palette-1") (let [[id _] (manage-prompts/selected-palette app-after)] id))))))
