(ns adnotare.core.event.app-test
  (:require
   [adnotare.core.event :as event]
   [adnotare.core.event.app]
   [adnotare.test.constants :refer [default-state]]
   [adnotare.util.uuid :as uuid]
   [clojure.test :refer [deftest is testing]]))

(deftest app-events
  (testing "add and clear toast"
    (let [toast-id (uuid/named "toast-1")
          toast {:toast/text "hello" :toast/type :info :toast/duration-ms 10 :toast/created-at-ms 1}
          added (event/handle default-state {:event/type :app/add-toast
                                             :id toast-id
                                             :toast toast})
          cleared (event/handle (:state added) {:event/type :app/clear-toast
                                                :id toast-id})]
      (is (= toast (get-in added [:state :state/ui :ui/toasts :by-id toast-id])))
      (is (nil? (get-in cleared [:state :state/ui :ui/toasts :by-id toast-id])))))
  (testing "start requests palettes load"
    (is (= {:on-load {:event/type :app/on-palettes-loaded}}
           (:load-palettes (event/handle default-state {:event/type :app/start})))))
  (testing "on-palettes-loaded success and error produce toast and initialized state"
    (let [ok-result (event/handle default-state {:event/type :app/on-palettes-loaded
                                                 :status :ok
                                                 :palettes (:state/palettes default-state)})
          err-result (event/handle default-state {:event/type :app/on-palettes-loaded
                                                  :status :error
                                                  :reason "boom"})]
      (is (= :success (get-in ok-result [:toast :toast/type])))
      (is (= :error (get-in err-result [:toast :toast/type])))
      (is (true? (get-in ok-result [:state :state/ui :ui/initialized?])))))
  (testing "navigate syncs manage prompts on route"
    (let [result (event/handle default-state {:event/type :app/navigate
                                              :route :manage-prompts})]
      (is (= :manage-prompts (get-in result [:state :state/ui :ui/route])))
      (is (= (get-in default-state [:state/ui :ui/annotate :annotate/active-palette-id])
             (get-in result [:state :state/ui :ui/manage-prompts :manage-prompts/selected-palette-id])))
      (is (nil? (get-in result [:state :state/ui :ui/manage-prompts :manage-prompts/selected-prompt-id]))))))
