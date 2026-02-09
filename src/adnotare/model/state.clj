(ns adnotare.model.state
  (:require
   [adnotare.model.session :as session]
   [adnotare.util.uuid :as uuid]))

(def initial
  {:state/session
   {:palettes {:by-id {}
               :last-used-ms {}}
    :annotate {:doc {:text ""}
               :annotations {:by-id {}
                             :selected-id nil}
               :active-palette-id nil}}
   :state/app {:route :annotate
               :toasts {:by-id {}}
               :initialized? false}})

(def default-palette
  {:label "Default"
   :prompts {:by-id
             {(uuid/named "default-prompt-1") {:text "Comment" :color 0}
              (uuid/named "default-prompt-2") {:text "Explain" :color 3}
              (uuid/named "default-prompt-3") {:text "Provide evidence" :color 7}
              (uuid/named "default-prompt-4") {:text "Provide example" :color 4}
              (uuid/named "default-prompt-5") {:text "User answer" :color 1}
              (uuid/named "default-prompt-6") {:text "Rephrase" :color 9}}
             :order [(uuid/named "default-prompt-1")
                     (uuid/named "default-prompt-3")
                     (uuid/named "default-prompt-2")
                     (uuid/named "default-prompt-4")
                     (uuid/named "default-prompt-5")
                     (uuid/named "default-prompt-6")]}})

(def default
  (-> initial
      (assoc-in [:state/session :palettes :by-id (uuid/named "default-palette")] default-palette)
      (update-in [:state/session] session/activate-last-used-palette)))
