(ns adnotare.app.manage-prompts.subs
  (:require
   [adnotare.model.schema :as S]
   [adnotare.model.session :as session]
   [cljfx.api :as fx]
   [malli.core :as m]))

(defn palette-options [context]
  (fx/sub-val context (comp session/palette-options :state/session)))
(m/=> palette-options [:-> S/Context [:sequential S/Option]])

(defn selected-palette-id [context]
  (fx/sub-val context (comp session/manage-prompts-selected-palette-id :state/session)))
(m/=> selected-palette-id [:-> S/Context [:maybe :uuid]])

(defn palette-id [context]
  (fx/sub-ctx context selected-palette-id))
(m/=> palette-id [:-> S/Context [:maybe :uuid]])

(defn palette [context]
  (fx/sub-val context (comp session/manage-prompts-palette :state/session)))
(m/=> palette [:-> S/Context [:maybe S/Palette]])

(defn active-prompts [context]
  (some-> (fx/sub-ctx context palette) :prompts))
(m/=> active-prompts [:-> S/Context [:maybe [:sequential S/Prompt]]])

(defn selected-prompt-id [context]
  (fx/sub-val context (comp session/manage-prompts-selected-prompt-id :state/session)))
(m/=> selected-prompt-id [:-> S/Context [:maybe :uuid]])

(defn selected-prompt [context]
  (fx/sub-val context (comp session/manage-prompts-selected-prompt :state/session)))
(m/=> selected-prompt [:-> S/Context [:maybe S/Prompt]])
