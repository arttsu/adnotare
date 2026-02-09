(ns adnotare.app.events
  (:require [adnotare.fx.handler :refer [handle-event]]
            [adnotare.model.ui :as ui]
            [cljfx.api :as fx])
  (:import (javafx.application Platform)))

(defmethod handle-event :app/quit [_]
  (Platform/exit)
  (System/exit 0))

(defmethod handle-event :app/add-toast [{:keys [fx/context id toast]}]
  {:context (fx/swap-context context ui/add-toast id toast)})

(defmethod handle-event :app/clear-toast [{:keys [fx/context id]}]
  {:context (fx/swap-context context ui/clear-toast id)})
