(ns adnotare.app.annotate.subs
  (:require
   [adnotare.core.derive.annotate :as derive.annotate]
   [adnotare.core.derive.palettes :as derive.palettes]
   [adnotare.core.state.ui.annotate :as ui.annotate]
   [cljfx.api :as fx]))

(defn doc-rich-text [context]
  (let [model (fx/sub-val context derive.annotate/doc-rich-text)]
    {:text (:rich-text/text model)
     :spans (mapv (fn [{:span/keys [start end style-classes]}]
                    {:start start
                     :end end
                     :style-classes style-classes})
                  (:rich-text/spans model))}))

(defn active-prompts [context]
  (fx/sub-val context derive.palettes/active-prompts))

(defn palette-options [context]
  (fx/sub-val context derive.palettes/palette-options))

(defn active-palette-id [context]
  (fx/sub-val context ui.annotate/active-palette-id))

(defn annotations [context]
  (fx/sub-val context derive.annotate/annotations))

(defn any-annotations? [context]
  (boolean (seq (fx/sub-val context derive.annotate/annotations))))

(defn selected-annotation [context]
  (fx/sub-val context derive.annotate/selected-annotation))

(defn selected-annotation-note [context]
  (some-> (fx/sub-ctx context selected-annotation) :annotation/note))

(defn selected-annotation-selection [context]
  (some-> (fx/sub-ctx context selected-annotation)
          :annotation/selection
          ((fn [{:selection/keys [start end text]}]
             {:start start :end end :text text}))))

(defn annotations-str [context]
  (fx/sub-val context derive.annotate/annotations-str))
