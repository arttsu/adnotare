(ns adnotare.test.hooks
  (:require
   [malli.dev :as dev]
   [malli.dev.pretty :as pretty]))

(defn malli-dev-start! [_test-plan]
  (dev/start! {:report (fn [type data]
                         ((pretty/reporter) type data)
                         (throw (ex-info "Boom!" data)))})

  _test-plan)

(defn malli-dev-stop! [_test-plan]
  (dev/stop!)
  _test-plan)
