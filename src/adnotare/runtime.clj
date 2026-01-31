(ns adnotare.runtime
  (:import (org.fxmisc.richtext CodeArea)))

(defonce *rich-area (atom nil))

(defn set-rich-area! [^CodeArea area]
  (reset! *rich-area area))

(defn clear-rich-area-selection! []
  (when-let [^CodeArea area @*rich-area]
    (.deselect area)
    (.requestFocus area)))
