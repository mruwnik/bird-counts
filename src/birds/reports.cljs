(ns birds.reports
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [birds.time :as time]
            [birds.events :as events]))

(defonce raw-events (r/atom []))
(defonce song-stats (r/atom {}))

(defn clear! []
  (reset! raw-events [])
  (reset! song-stats {}))

(defn update-stats [{:keys [event-type motivated-singing]}]
  (if (= event-type :start-singing)
    (swap! song-stats update (if motivated-singing :motivated-singing :spontaneous-singing) inc)))

(defn add-raw-event [event]
  (when-not (-> event :event-type #{:singing :re-sing})
    (swap! raw-events conj (assoc event :tick (time/now)))))

(defn init! [settings]
  (events/attach-listener update-stats)
  (events/attach-listener add-raw-event))

(defn event->row [event]
  (->> [(-> event :bird-id)
        (-> event :tick)
        (-> event :event-type)
        (-> event :pos :x)
        (-> event :pos :y)
        (-> event :volume)
        (-> event :song-length)]
       (map #(if % (str %) ""))
       (str/join ",")))

(defn csv-raw-events []
  (let [header (str/join "," ["bird-id" "tick" "event-type" "pos-x" "pos-y" "volume" "song-length"])
        data (map event->row @raw-events)]
    [(str/join "\n" (concat [header] data))]))

(defn get-raw-data [type]
  (let [mime-type ({:csv "text/csv"
                    :json "application/json"} type)]
    (js/Blob. (condp = type
                :json (->> @raw-events clj->js (.stringify js/JSON) vector)
                :csv (csv-raw-events))
              {:type mime-type})))

(defn download-raw-events [file-type]
  (let [data-blob (get-raw-data file-type)
        link (.createElement js/document "a")]
    (set! (.-href link) (.createObjectURL js/URL data-blob))
    (.setAttribute link "download" (->> file-type name (str "raw-events." )))
    (.appendChild (.-body js/document) link)
    (.click link)
    (.removeChild (.-body js/document) link)))

(defn stats []
  [:div
   (doall
    (for [[key desc] [[:spontaneous-singing "spontaneous sung"]
                      ;; [:stop-singing "stopped singing"]
                      [:motivated-singing "motivated sung"]]]
      [:div {:class :stat :key key}
       [:span {:class :desc} desc]
       [:span {:class :amount} (key @song-stats)]]))])

(defn show [settings]
  [:div {:class :reports}
   "Stats:"
   [:br] [:button {:on-click clear!} "Clear data"]
   [stats]
   [:div {:class :events-downloader}
    "Download raw events"
    [:button {:on-click #(download-raw-events :csv)} "csv"]
    [:button {:on-click #(download-raw-events :json)} "json"]]])
