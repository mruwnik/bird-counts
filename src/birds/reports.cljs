(ns birds.reports
  (:require [reagent.core :as r]
            [birds.events :as events]
            [cljs.core.async :as async]))

(defonce song-stats (r/atom {}))
(defonce tick-count (r/atom 0))
(defonce ticker (js/setInterval #(swap! tick-count inc) 1000))

(defn handle-event [event]
  (when (= (:event-type event) :die)
    (prn "die"))
  (when (= (:event-type event) :re-sing)
    (prn event))
  (swap! song-stats update (:event-type event) inc))

(defn init! [settings]
  (swap! song-stats assoc :listener (events/attach-listener handle-event)))


(defn stats []
  (let [tick @tick-count]
    [:div
     ;; tick
     [:input {:type :hidden :value tick}]
     (doall
      (for [[key desc] [[:start-singing "spontaneous sung"]
                        ;; [:stop-singing "stopped singing"]
                        [:re-sing "motivated sung"]]]
        [:div {:class :stat :key key}
         [:span {:class :desc} desc]
         [:span {:class :amount} (key @song-stats)]]))]))

(defn show [settings]
  [:div {:class :reports}
   "Stats:"
   [stats]

     ])
