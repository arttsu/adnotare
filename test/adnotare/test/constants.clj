(ns adnotare.test.constants
  (:require [adnotare.util.uuid :refer [uuid]]))

(def default-prompt-palette
  {:label "Default"
   :prompts {:by-id
             {(uuid "default-prompt-1") {:text "Generic" :color 0}
              (uuid "default-prompt-2") {:text "Explain this" :color 5}
              (uuid "default-prompt-3") {:text "Are you sure about this?" :color 8}
              (uuid "default-prompt-4") {:text "Give more details" :color 3}
              (uuid "default-prompt-5") {:text "My answer" :color 2}}
             :order [(uuid "default-prompt-1")
                     (uuid "default-prompt-3")
                     (uuid "default-prompt-2")
                     (uuid "default-prompt-4")
                     (uuid "default-prompt-5")]}})

(def default-state
  {:prompt-palettes {:by-id {(uuid "default-palette") default-prompt-palette}}
   :session {:doc {:text "Hello World! This is a test of Adnotare."}
             :annotations {:by-id {(uuid "ann-1") {:prompt-ref {:palette-id (uuid "default-palette") :prompt-id (uuid "default-prompt-2")}
                                                   :selection {:start 13 :end 27 :text "This is a test"}
                                                   :note ""}
                                   (uuid "ann-2") {:prompt-ref {:palette-id (uuid "default-palette") :prompt-id (uuid "default-prompt-4")}
                                                   :selection {:start 31 :end 39 :text "Adnotare"}
                                                   :note "Etymology"}}
                           :selected-id (uuid "ann-2")}
             :active-palette-id (uuid "default-palette")}
   :ui {:route :annotate
        :toasts {:by-id {}}}})
