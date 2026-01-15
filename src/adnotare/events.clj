(ns adnotare.events)

(defmulti event-handler :event/type)
