(ns birds.gui
  (:require [reagent.core :as r]
            [birds.forest :as forest]
            [birds.events :as events]))

(defonce settings (r/atom {:container-name "forest"
                           :frame-rate 30
                           :num-of-birds 10
                           :volume 25
                           :spontaneous-sing-prob 0.01 ;; in a given 0.1s
                           :motivated-sing-prob 0.9
                           :motivated-sing-after 3000
                           :sing-rest-time 10000
                           :song-length 2000
                           :audio-sensitivity 50
                           :show-bird-hear? true}))

(defn generate-birds! [settings]
  (->> @settings
       :num-of-birds
       range
       (map (partial events/random-bird events/broadcast-chan @settings))
       (swap! settings assoc :birds)))

(defn initialise! []
  (let [container (.getElementById js/document (:container-name @settings))
        width     (-> container .-clientWidth (- 40))]
    (swap! settings assoc
           :container container
           :width width
           :height (min width (-> js/document .-body .-clientHeight)))
    (generate-birds! settings)))

(defn num-input
  ([key desc] (num-input key desc #(js/parseInt % 10) (partial forest/bird-updater key)))
  ([key desc parser func]
   [:div {:class [:setting-input key]}
    desc
    [:input {:type "number"
             :value (key @settings)
             :on-change #(func (swap! settings assoc key (-> % .-target .-value parser)))}]]))

(defn int-input
  ([key desc] (int-input key desc (partial forest/bird-updater key)))
  ([key desc func] (num-input key desc #(js/parseInt % 10) func)))

(defn prob-input
  [key desc]
  [:div {:class [:setting-input key]}
   desc
   [:input {:type "number"
            :min "0"
            :max "1"
            :step "0.01"
            :value (key @settings)
            :on-change #(forest/bird-updater key (swap! settings assoc key (-> % .-target .-value js/parseFloat)))}]])

(defn ms-input
  [key desc]
  [:div {:class [:setting-input key]}
   desc
   [:input {:type "number"
            :min "0"
            :step "1000"
            :value (key @settings)
            :on-change #(forest/bird-updater key (swap! settings assoc key (-> % .-target .-value js/parseFloat)))}]])

(defn checkbox [key desc]
  [:div {:class [:setting-input key]}
   desc
   [:input {:type "checkbox"
            :checked (key @settings)
            :on-change #(forest/bird-updater key (swap! settings assoc key (-> % .-target .-checked)))}]])

(defn controls []
  [:div {:class :controls}
   [int-input :num-of-birds "Number of birds: "
    (fn [new-settings]
      (doseq [bird (:birds new-settings)]
        (events/kill! bird))
      (generate-birds! settings))]
   [int-input :volume "Bird volume"]
   [int-input :audio-sensitivity "how far birds can hear"]
   [prob-input :spontaneous-sing-prob "Prob of singing in a given 0.1s"]
   [prob-input :motivated-sing-prob "Prob of motivated singing"]
   [ms-input :motivated-sing-after "avg time to motivated singing [ms]"]
   [ms-input :sing-rest-time "avg time of resting after singing [ms]"]
   [ms-input :song-length "avg song length [ms]"]
   [checkbox :show-birds? "show bird location?"]
   [checkbox :show-bird-hear? "show bird hearing radius?"]])
