(ns adnotare.handler
  (:require [adnotare.events :as events]
            [adnotare.runtime :as rt]
            [cljfx.api :as fx]
            [clojure.core.cache :as cache])
  (:import (javafx.scene.control Alert Alert$AlertType ButtonType)
           (javafx.scene.input Clipboard ClipboardContent)
           (javafx.animation PauseTransition)
           (javafx.util Duration)
           (javafx.event EventHandler)))

(def *state
  (atom (fx/create-context {:text "Hello, World! This is a test of Adnotare."
                            :annotation-kinds {"000" {:color "00" :text "Generic"}
                                               "AAA" {:color "01" :text "Please explain this part"}
                                               "BBB" {:color "09" :text "Are you sure about this?"}
                                               "CCC" {:color "05" :text "Give me more details"}
                                               "DDD" {:color "03" :text "Answer"}}
                            :annotations {}
                            :selected-annotation-id nil
                            :rich-area-selection {:start 0
                                                  :end 0
                                                  :selected-text ""}
                            :toasts {}}
                           cache/lru-cache-factory)))

(def event-handler
  (-> events/event-handler
      (fx/wrap-co-effects
       {:fx/context (fx/make-deref-co-effect *state)})
      (fx/wrap-effects
       {:context (fx/make-reset-effect *state)
        :dispatch fx/dispatch-effect
        :clear-rich-area-selection (fn [_] (rt/clear-rich-area-selection!))
        :confirm (fn [{:keys [title header content yes-event]} dispatch]
                   (let [alert (doto (Alert. Alert$AlertType/CONFIRMATION)
                                 (.setTitle title)
                                 (.setHeaderText header)
                                 (.setContentText content))
                         res (.orElse (.showAndWait alert) ButtonType/CANCEL)]
                     (when (= res ButtonType/OK)
                       (dispatch yes-event))))
        :paste-from-clipboard (fn [_ dispatch]
                                (let [cb (Clipboard/getSystemClipboard)
                                      s (when (.hasString cb) (.getString cb))]
                                  (when (some? s)
                                    (dispatch {:event/type :adnotare/swap-text
                                               :text s}))))
        :copy-to-clipboard (fn [{:keys [text]} _dispatch]
                             (let [cb (Clipboard/getSystemClipboard)
                                   content (doto (ClipboardContent.)
                                             (.putString (or text "")))]
                               (.setContent cb content)))
        :dispatch-later (fn [{:keys [ms event]} dispatch]
                          (let [pt (PauseTransition. (Duration/millis (double (or ms 0))))]
                            (.setOnFinished pt
                                            (reify EventHandler
                                              (handle [_ _]
                                                (dispatch event))))
                            (.play pt)))})))
