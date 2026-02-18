(ns adnotare.test.core.factory
  (:require
   [adnotare.core.model.annotation :as annotation]
   [adnotare.core.model.prompt-ref :as prompt-ref]
   [adnotare.core.model.selection :as selection]
   [adnotare.core.model.toast :as toast]
   [adnotare.core.util.uuid :as uuid]
   [clojure.string :as string]))

(defn ->annotation [{:keys [palette-id prompt-id start end quote note] :or {palette-id (uuid/random)
                                                                            prompt-id (uuid/random)
                                                                            note ""
                                                                            start 0
                                                                            end 10}}]
  (let [quote-with-fallback (if (and start end (nil? quote))
                              (string/join (repeat (- end start) "x"))
                              quote)]
    {::annotation/prompt-ref {::prompt-ref/palette-id palette-id ::prompt-ref/prompt-id prompt-id}
     ::annotation/selection {::selection/start start ::selection/end end ::selection/quote quote-with-fallback}
     ::annotation/note note}))

(defn ->prompt-ref [palette-id prompt-id]
  {::prompt-ref/palette-id palette-id ::prompt-ref/prompt-id prompt-id})

(defn ->selection [{:keys [start end quote from]}]
  (let [quote' (or quote (subs from start end))]
    {::selection/start start ::selection/end end ::selection/quote quote'}))

(defn ->toast [{:keys [type text created-at-ms] :or {type ::toast/success created-at-ms 0}}]
  {::toast/type type ::toast/text text ::toast/created-at-ms created-at-ms})

(defn ->persisted-palettes [version palettes]
  (let [palettes' (reduce (fn [acc [id palette last-used-ms]]
                            (cond-> (assoc-in acc [:by-id id] palette)
                              last-used-ms (assoc-in [:last-used-ms id] last-used-ms)))
                          {:by-id {} :last-used-ms {}}
                          palettes)]
    {:version version, :data palettes'}))
