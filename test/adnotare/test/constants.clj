(ns adnotare.test.constants
  (:require [adnotare.util.uuid :as uuid]))

(def default-prompt-palette
  {:label "Default"
   :prompts
   {:by-id
    {(uuid/named "default-prompt-1") {:text "Generic" :color 0}
     (uuid/named "default-prompt-2") {:text "Explain this" :color 5}
     (uuid/named "default-prompt-3") {:text "Are you sure about this?" :color 8}
     (uuid/named "default-prompt-4") {:text "Give more details" :color 3}
     (uuid/named "default-prompt-5") {:text "User answer" :color 2}}
    :order [(uuid/named "default-prompt-1")
            (uuid/named "default-prompt-3")
            (uuid/named "default-prompt-2")
            (uuid/named "default-prompt-4")
            (uuid/named "default-prompt-5")]}})

(def default-state
  {:state/session
   {:palettes
    {:by-id
     {(uuid/named "default-palette") default-prompt-palette}
     :last-used-ms {}}
    :annotate
    {:doc {:text "Hello World! This is a test of Adnotare."}
     :annotations
     {:by-id
      {(uuid/named "ann-1") {:prompt-ref {:palette-id (uuid/named "default-palette") :prompt-id (uuid/named "default-prompt-2")}
                       :selection {:start 13 :end 27 :text "This is a test"}
                       :note ""}
       (uuid/named "ann-2") {:prompt-ref {:palette-id (uuid/named "default-palette") :prompt-id (uuid/named "default-prompt-4")}
                       :selection {:start 31 :end 39 :text "Adnotare"}
                       :note "Etymology"}}
      :selected-id (uuid/named "ann-2")}
     :active-palette-id (uuid/named "default-palette")}}
   :state/app
   {:initialized? true
    :route :annotate
    :toasts {:by-id {}}}})

(def default-session (:state/session default-state))
