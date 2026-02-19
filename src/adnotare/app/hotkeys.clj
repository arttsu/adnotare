(ns adnotare.app.hotkeys
  (:require
   [clojure.string :as string]))

(def ^:private action->binding
  {::paste "Shortcut+Shift+V"
   ::copy-annotations "Shortcut+Shift+C"
   ::copy-annotations+document "Shortcut+Alt+Shift+C"
   ::palette-prev "Shortcut+Shift+P"
   ::palette-next "Shortcut+Shift+N"})

(defn shortcut-name []
  (if (string/includes? (System/getProperty "os.name" "") "Mac")
    "Cmd"
    "Ctrl"))

(defn hotkey-label [action]
  (some-> (get action->binding action)
          (string/replace "Shortcut" (shortcut-name))))

(defn prompt-hotkey-label [idx]
  (when (and (int? idx) (<= 0 idx 9))
    (str (shortcut-name) "+Alt+" (if (= idx 9) "0" (inc idx)))))
