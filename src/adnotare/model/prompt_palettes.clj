(ns adnotare.model.prompt-palettes
  (:require [adnotare.model.schema :as S]
            [malli.core :as m]))

(defn prompt-by-ref [state {:keys [palette-id prompt-id]}]
  (get-in state [:prompt-palettes :by-id palette-id :prompts :by-id prompt-id]))
(m/=> prompt-by-ref [:-> S/State S/PromptRef S/Prompt])

(defn palette-by-id [state id]
  (get-in state [:prompt-palettes :by-id id]))
(m/=> palette-by-id [:-> S/State :uuid S/PromptPalette])
