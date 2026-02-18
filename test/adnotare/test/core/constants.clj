(ns adnotare.test.core.constants
  (:require
   [adnotare.core.model.annotation :as annotation]
   [adnotare.core.model.annotator :as annotator]
   [adnotare.core.model.app :as app]
   [adnotare.core.model.document :as document]
   [adnotare.core.model.palette :as palette]
   [adnotare.core.model.prompt :as prompt]
   [adnotare.core.model.prompt-manager :as prompt-manager]
   [adnotare.core.model.prompt-ref :as prompt-ref]
   [adnotare.core.model.selection :as selection]
   [adnotare.core.util.uuid :as uuid]))

(def palette-1
  {::palette/label "Palette One"
   ::palette/prompts
   {:by-id
    {(uuid/named "prompt-11") {::prompt/text "Comment" ::prompt/color 0}
     (uuid/named "prompt-12") {::prompt/text "Explain" ::prompt/color 3}
     (uuid/named "prompt-13") {::prompt/text "Provide evidence" ::prompt/color 7}
     (uuid/named "prompt-14") {::prompt/text "Give example" ::prompt/color 4}
     (uuid/named "prompt-15") {::prompt/text "User answer" ::prompt/color 1}}
    :order
    [(uuid/named "prompt-11")
     (uuid/named "prompt-15")
     (uuid/named "prompt-14")
     (uuid/named "prompt-12")
     (uuid/named "prompt-13")]}})

(def palette-2
  {::palette/label "Palette Two"
   ::palette/prompts
   {:by-id
    {(uuid/named "prompt-21") {::prompt/text "Prompt One" ::prompt/color 0}
     (uuid/named "prompt-22") {::prompt/text "Prompt Two" ::prompt/color 1}
     (uuid/named "prompt-23") {::prompt/text "Prompt Three" ::prompt/color 2}}
    :order
    [(uuid/named "prompt-21")
     (uuid/named "prompt-22")
     (uuid/named "prompt-23")]}})

(def palette-3
  {::palette/label "Palette Three"
   ::palette/prompts
   {:by-id
    {(uuid/named "prompt-31") {::prompt/text "Comment" ::prompt/color 0}}
    :order
    [(uuid/named "prompt-31")]}})

(def default-palettes
  {:by-id
   {(uuid/named "palette-1") palette-1
    (uuid/named "palette-2") palette-2
    (uuid/named "palette-3") palette-3}
   :last-used-ms
   {(uuid/named "palette-1") 1000}})

(def default-app
  {::app/initialized? true
   ::app/init-errors {}
   ::app/route ::app/annotator
   ::app/toasts {:by-id {}}
   ::app/palettes default-palettes
   ::app/document
   {::document/text "Hello World! This is a test of Adnotare."
    ::document/annotations
    {:by-id
     {(uuid/named "annotation-1") {::annotation/prompt-ref {::prompt-ref/palette-id (uuid/named "palette-1")
                                                            ::prompt-ref/prompt-id (uuid/named "prompt-13")}
                                   ::annotation/selection {::selection/start 13 ::selection/end 27 ::selection/quote "This is a test"}
                                   ::annotation/note ""}
      (uuid/named "annotation-2") {::annotation/prompt-ref {::prompt-ref/palette-id (uuid/named "palette-1")
                                                            ::prompt-ref/prompt-id (uuid/named "prompt-12")}
                                   ::annotation/selection {::selection/start 31 ::selection/end 39 ::selection/quote "Adnotare"}
                                   ::annotation/note "Etymology"}}}}
   ::app/annotator
   {::annotator/active-palette-id (uuid/named "palette-1")
    ::annotator/selected-annotation-id (uuid/named "annotation-2")}
   ::app/prompt-manager
   {::prompt-manager/selected-palette-id nil
    ::prompt-manager/selected-prompt-id nil}})
