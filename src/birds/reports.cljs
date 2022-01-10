(ns birds.reports
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [birds.views.events :as event]
            [birds.views.downloader :as downloader]
            [birds.time :as time]))

(defonce raw-events (r/atom []))
(defonce song-stats (r/atom {}))

(defn clear! []
  (reset! raw-events [])
  (reset! song-stats {}))

(defn update-stats [{:keys [event-type motivated-singing]}]
  (when (= event-type :start-singing)
    (swap! song-stats update (if motivated-singing :motivated-singing :spontaneous-singing) inc)))

(defn add-raw-event [event]
  (when-not (-> event :event-type #{:singing :re-sing})
    (swap! raw-events conj (assoc event :tick (time/now)))))

(defn init! []
  (re-frame/dispatch [::event/attach-event-listener update-stats])
  (re-frame/dispatch [::event/attach-event-listener add-raw-event]))

(defn event->data-point [event]
  (-> event
      (select-keys [:bird-id :tick :event-type :volume :song-length])
      (assoc :pos-x (-> event :pos :x)
             :pos-y (-> event :pos :x)
             :motivated (if (-> event :motivated-singing) 1 0))))

(defn download-raw-events [file-type]
  (downloader/download-data "events" file-type
                 [:bird-id :tick :event-type :pos-x :pos-y :volume :song-length :motivated]
                 (map event->data-point @raw-events)))

(defn stats []
  [:div
   (doall
    (for [[key desc] [[:spontaneous-singing "spontaneous sung"]
                      ;; [:stop-singing "stopped singing"]
                      [:motivated-singing "motivated sung"]]]
      [:div {:class :stat :key key}
       [:span {:class :desc} desc]
       [:span {:class :amount} (key @song-stats)]]))])

(defn show []
  [:div {:class :reports}
   "Stats:"
   [:br] [:button {:on-click clear!} "Clear data"]
   [stats]
   [:div {:class :events-downloader}
    "Download all events"
    [:button {:on-click #(download-raw-events :csv)} "csv"]
    [:button {:on-click #(download-raw-events :json)} "json"]]])
