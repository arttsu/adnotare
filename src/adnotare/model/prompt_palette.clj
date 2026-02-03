(ns adnotare.model.prompt-palette
  (:require [adnotare.model.schema :as S]
            [malli.core :as m]))

(defn prompt-order [palette]
  (get-in palette [:prompts :order]))

(defn sorted-prompts [palette]
  (let [{:keys [by-id order]} (:prompts palette)]
    (map (fn [id] (assoc (get by-id id) :id id)) order)))
(m/=> sorted-prompts [:-> S/PromptPalette [:sequential S/DenormPrompt]])
