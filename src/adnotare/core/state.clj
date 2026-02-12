(ns adnotare.core.state
  (:require
   [adnotare.core.schema :as S]
   [adnotare.core.state.palettes :as state.palettes]
   [adnotare.core.state.ui :as state.ui]
   [adnotare.core.state.ui.annotate :as state.ui.annotate]
   [malli.core :as m]
   [adnotare.util.uuid :as uuid]))

(def default-palette
  {:palette/label "Default"
   :palette/prompts {:by-id
                     {(uuid/named "default-prompt-1") {:prompt/text "Comment" :prompt/color 0}
                      (uuid/named "default-prompt-2") {:prompt/text "Explain" :prompt/color 3}
                      (uuid/named "default-prompt-3") {:prompt/text "Provide evidence" :prompt/color 7}
                      (uuid/named "default-prompt-4") {:prompt/text "Provide example" :prompt/color 4}
                      (uuid/named "default-prompt-5") {:prompt/text "User answer" :prompt/color 1}
                      (uuid/named "default-prompt-6") {:prompt/text "Rephrase" :prompt/color 9}}
                     :order [(uuid/named "default-prompt-1")
                             (uuid/named "default-prompt-3")
                             (uuid/named "default-prompt-2")
                             (uuid/named "default-prompt-4")
                             (uuid/named "default-prompt-5")
                             (uuid/named "default-prompt-6")]}})

(def default-palettes
  {:palettes/by-id {(uuid/named "default-palette") default-palette}
   :palettes/last-used-ms {}})

(def initial
  {:state/document {:document/text ""
                    :document/annotations {:by-id {}}}
   :state/palettes {:palettes/by-id {}
                    :palettes/last-used-ms {}}
   :state/ui {:ui/initialized? false
              :ui/route :annotate
              :ui/toasts {:by-id {}}
              :ui/annotate {:annotate/active-palette-id nil
                            :annotate/selected-annotation-id nil}
              :ui/manage-prompts {:manage-prompts/selected-palette-id nil
                                  :manage-prompts/selected-prompt-id nil}}})

;; PR: Unused
(defn initialize [state palettes]
  (let [state (state.palettes/put-palettes state palettes)
        active-id (or (state.palettes/most-recent-id state)
                      (state.palettes/first-palette-id state))]
    (-> state
        (state.ui.annotate/set-active-palette active-id)
        (state.ui/set-initialized true))))
(m/=> initialize [:=> [:cat S/State S/Palettes] S/State])
