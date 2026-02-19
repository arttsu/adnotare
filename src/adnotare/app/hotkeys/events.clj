(ns adnotare.app.hotkeys.events
  (:require
   [adnotare.app.annotator.subs :as annotator-subs]
   [adnotare.app.interface :refer [handle-event]]
   [adnotare.app.subs :as app-subs]
   [adnotare.core.model.app :as app])
  (:import
   (javafx.scene.input KeyCode KeyEvent)))

(defn- code= [^KeyEvent e code]
  (= (.getCode e) code))

(defn- match?
  [^KeyEvent e {:keys [shortcut shift alt code]}]
  (and (= (boolean shortcut) (.isShortcutDown e))
       (= (boolean shift) (.isShiftDown e))
       (= (boolean alt) (.isAltDown e))
       (code= e code)))

(defn- shortcut-digit->idx [^KeyEvent e]
  (when (and (.isShortcutDown e)
             (.isAltDown e))
    (case (.getCode e)
      KeyCode/DIGIT1 0
      KeyCode/DIGIT2 1
      KeyCode/DIGIT3 2
      KeyCode/DIGIT4 3
      KeyCode/DIGIT5 4
      KeyCode/DIGIT6 5
      KeyCode/DIGIT7 6
      KeyCode/DIGIT8 7
      KeyCode/DIGIT9 8
      KeyCode/DIGIT0 9
      KeyCode/NUMPAD1 0
      KeyCode/NUMPAD2 1
      KeyCode/NUMPAD3 2
      KeyCode/NUMPAD4 3
      KeyCode/NUMPAD5 4
      KeyCode/NUMPAD6 5
      KeyCode/NUMPAD7 6
      KeyCode/NUMPAD8 7
      KeyCode/NUMPAD9 8
      KeyCode/NUMPAD0 9
      nil)))

(defn- shortcut-alt-text-digit->idx [^KeyEvent e]
  (when (and (.isShortcutDown e)
             (.isAltDown e))
    (case (.getText e)
      "1" 0, "¡" 0
      "2" 1, "™" 1
      "3" 2, "£" 2
      "4" 3, "¢" 3
      "5" 4, "∞" 4
      "6" 5, "§" 5
      "7" 6, "¶" 6
      "8" 7, "•" 7
      "9" 8, "ª" 8
      "0" 9, "º" 9
      nil)))

(defn- prompt-hotkey-idx [^KeyEvent e]
  (or (shortcut-digit->idx e)
      (shortcut-alt-text-digit->idx e)))

(defn- annotator-hotkey-dispatch [context ^KeyEvent e]
  (cond
    (match? e {:shortcut true :shift true :alt false :code KeyCode/V})
    {:event/type :annotator/paste-document-from-clipboard}

    (match? e {:shortcut true :shift true :alt false :code KeyCode/C})
    {:event/type :annotator/copy-annotations-as-llm-prompt}

    (match? e {:shortcut true :shift true :alt true :code KeyCode/C})
    {:event/type :annotator/copy-annotations-and-document-as-llm-prompt}

    (match? e {:shortcut true :shift true :alt false :code KeyCode/P})
    {:event/type :annotator/switch-palette-prev}

    (match? e {:shortcut true :shift true :alt false :code KeyCode/N})
    {:event/type :annotator/switch-palette-next}

    :else
    (when-let [idx (prompt-hotkey-idx e)]
      (when-let [[prompt-id _prompt] (nth (vec (annotator-subs/active-prompts context)) idx nil)]
        {:event/type :annotator/add-annotation-from-selection
         :prompt-id prompt-id}))))

(defmethod handle-event :hotkeys/key-pressed [{:keys [fx/context fx/event]}]
  (let [route (app-subs/route context)
        dispatch-event (case route
                         ::app/annotator (annotator-hotkey-dispatch context event)
                         ::app/prompt-manager nil
                         nil)]
    (when dispatch-event
      {:consume-event event
       :dispatch dispatch-event})))
