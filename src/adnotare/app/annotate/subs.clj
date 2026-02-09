(ns adnotare.app.annotate.subs
  (:require [adnotare.model.prompt-palette :as palette]
            [adnotare.model.prompt-palettes :as palettes]
            [adnotare.model.session :as session]
            [cljfx.api :as fx]))

(defn- annotations [context]
  (fx/sub-val context session/annotations))

(defn doc-rich-text [context]
  (fx/sub-val context session/doc-rich-text))

(defn active-palette-id [context]
  (fx/sub-val context session/active-palette-id))

(defn active-palette [context]
  (let [id (fx/sub-ctx context active-palette-id)]
    (fx/sub-val context palettes/palette-by-id id)))

(defn sorted-prompts [context]
  (let [active-palette (fx/sub-ctx context active-palette)]
    (palette/sorted-prompts active-palette)))

(defn sorted-annotations [context]
  (sort-by (comp :start :selection) (fx/sub-ctx context annotations)))

(defn any-annotations? [context]
  (let [dict (fx/sub-val context session/annotations-by-id)]
    (not (empty? dict))))

(defn selected-annotation-note [context]
  (fx/sub-val context session/selected-annotation-note))

(defn selected-annotation-selection [context]
  (when-let [annotation (fx/sub-val context session/selected-annotation)]
    (:selection annotation)))

(defn annotations-str [context]
  (fx/sub-val context session/annotations-str))
