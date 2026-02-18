(ns hooks.adnotare.core.util.result
  (:require [clj-kondo.hooks-api :as api]))

(defn if-ok [{:keys [node]}]
  (let [[_ binding ok-branch & rest] (:children node)
        [sym ok-expr] (:children binding)

        res-sym (api/token-node 'res__kondo)

        ok-test
        (api/list-node
          [(api/token-node '=)
           (api/keyword-node :ok)
           (api/list-node
             [(api/keyword-node :status) res-sym])])

        ok-let
        (api/list-node
          [(api/token-node 'let)
           (api/vector-node
             [sym
              (api/list-node
                [(api/keyword-node :value) res-sym])])
           ok-branch])

        err-form
        (if (= 2 (count rest))
          ;; (if-ok [sym expr] ok-branch [err-sym] err-branch)
          (let [[err-binding err-branch] rest
                [err-sym] (:children err-binding)]
            (api/list-node
              [(api/token-node 'let)
               (api/vector-node
                 [err-sym
                  (api/list-node
                    [(api/keyword-node :reason) res-sym])])
               err-branch]))
          ;; (if-ok [sym expr] ok-branch err-branch)
          (first rest))]

    {:node
     (api/list-node
       [(api/token-node 'let)
        (api/vector-node [res-sym ok-expr])
        (api/list-node
          [(api/token-node 'if)
           ok-test
           ok-let
           err-form])])}))
