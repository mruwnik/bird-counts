(ns birds.reports
  (:require [reagent.core :as r]
            [birds.events :as events]
            [cljs.core.async :as async]))

(defonce song-stats (r/atom {}))
(defonce tick-count (r/atom 0))
(defonce ticker (js/setInterval #(swap! tick-count inc) 1000))

(defn handle-event [{:keys [event-type motivated-singing]}]
  (if (= event-type :start-singing)
    (swap! song-stats update (if motivated-singing :motivated-singing :spontaneous-singing) inc)))

(defn init! [settings]
  (swap! song-stats assoc :listener (events/attach-listener handle-event)))


(defn stats []
  (let [tick @tick-count]
    [:div
     ;; tick
     [:input {:type :hidden :value tick}]
     (doall
      (for [[key desc] [[:spontaneous-singing "spontaneous sung"]
                        ;; [:stop-singing "stopped singing"]
                        [:motivated-singing "motivated sung"]]]
        [:div {:class :stat :key key}
         [:span {:class :desc} desc]
         [:span {:class :amount} (key @song-stats)]]))]))

(defn show [settings]
  [:div {:class :reports}
   "Stats:"
   [stats]

     ])
