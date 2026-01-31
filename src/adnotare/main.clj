(ns adnotare.main
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [adnotare.events :as events]
            [adnotare.views :as views]
            [adnotare.runtime :as rt])
  (:import (javafx.scene.control Alert Alert$AlertType ButtonType)
           (javafx.scene.input Clipboard)))

(def *state
  (atom (fx/create-context {:text "Hello, World! This is a test of Adnotare."
                            :annotation-kinds {"000" {:color "00" :text "Generic"}
                                               "AAA" {:color "01" :text "Please explain this part"}
                                               "BBB" {:color "09" :text "Are you sure about this?"}}
                            :annotations {}
                            :selected-annotation-id nil
                            :rich-area-selection {:start 0
                                                  :end 0
                                                  :selected-text ""}}
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
                                               :text s}))))})))

(def renderer
  (fx/create-renderer
   :middleware (comp
                fx/wrap-context-desc
                (fx/wrap-map-desc (fn [_] {:fx/type views/root
                                           :adnotare/dispatch! event-handler})))
   :opts {:fx.opt/map-event-handler event-handler
          :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                       (fx/fn->lifecycle-with-context %))}))

(defn -main [& _args]
  (fx/mount-renderer *state renderer))
