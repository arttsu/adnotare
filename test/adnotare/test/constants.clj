(ns adnotare.test.constants
  (:require [adnotare.util.uuid :as uuid]))

(def default-prompt-palette
  {:palette/label "Default"
   :palette/prompts
   {:by-id
    {(uuid/named "default-prompt-1") {:prompt/text "Generic" :prompt/color 0}
     (uuid/named "default-prompt-2") {:prompt/text "Explain this" :prompt/color 5}
     (uuid/named "default-prompt-3") {:prompt/text "Are you sure about this?" :prompt/color 8}
     (uuid/named "default-prompt-4") {:prompt/text "Give more details" :prompt/color 3}
     (uuid/named "default-prompt-5") {:prompt/text "User answer" :prompt/color 2}}
    :order [(uuid/named "default-prompt-1")
            (uuid/named "default-prompt-3")
            (uuid/named "default-prompt-2")
            (uuid/named "default-prompt-4")
            (uuid/named "default-prompt-5")]}})

(def default-state
  {:state/document
   {:document/text "Hello World! This is a test of Adnotare."
    :document/annotations
    {:by-id
     {(uuid/named "ann-1") {:annotation/prompt-ref {:prompt-ref/palette-id (uuid/named "default-palette")
                                                    :prompt-ref/prompt-id (uuid/named "default-prompt-2")}
                            :annotation/selection {:selection/start 13 :selection/end 27 :selection/text "This is a test"}
                            :annotation/note ""}
      (uuid/named "ann-2") {:annotation/prompt-ref {:prompt-ref/palette-id (uuid/named "default-palette")
                                                    :prompt-ref/prompt-id (uuid/named "default-prompt-4")}
                            :annotation/selection {:selection/start 31 :selection/end 39 :selection/text "Adnotare"}
                            :annotation/note "Etymology"}}}}
   :state/palettes
   {:palettes/by-id {(uuid/named "default-palette") default-prompt-palette}
    :palettes/last-used-ms {}}
   :state/ui
   {:ui/initialized? true
    :ui/route :annotate
    :ui/toasts {:by-id {}}
    :ui/annotate {:annotate/active-palette-id (uuid/named "default-palette")
                  :annotate/selected-annotation-id (uuid/named "ann-2")}
    :ui/manage-prompts {:manage-prompts/selected-palette-id nil
                        :manage-prompts/selected-prompt-id nil}}})
