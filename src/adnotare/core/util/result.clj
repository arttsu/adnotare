(ns adnotare.core.util.result
  (:require
   [adnotare.core.util.schema :refer [ReadEDNFileError]]))

(defn Result [T E]
  [:multi {:dispatch :status}
   [:ok
    [:map
     [:status [:= :ok]]
     [:value T]]]
   [:error
    [:map
     [:status [:= :error]]
     [:reason E]]]])

(def ReadEDNFileResult (Result :map ReadEDNFileError))

(defmacro if-ok
  ([[sym ok-expr] ok-branch err-branch]
   `(let [res# ~ok-expr]
      (if (= :ok (:status res#))
        (let [~sym (:value res#)]
          ~ok-branch)
        ~err-branch)))
  ([[sym ok-expr] ok-branch [err-sym] err-branch]
   `(let [res# ~ok-expr]
      (if (= :ok (:status res#))
        (let [~sym (:value res#)]
          ~ok-branch)
        (let [~err-sym (:reason res#)]
          ~err-branch)))))

(defmacro when-ok [[sym expr] & body]
  `(let [res# ~expr]
     (when (= :ok (:status res#))
       (let [~sym (:value res#)]
         ~@body))))

(defn ok [value]
  {:status :ok, :value value})

(defn error [reason]
  {:status :error, :reason reason})
