(ns adnotare.app.interface)

(defmulti handle-event :event/type)
