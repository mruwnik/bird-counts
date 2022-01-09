(ns birds.views.views
  (:require [re-frame.core :as re-frame]
            [birds.views.subs :as subs]
            [birds.views.html :as html]
            [birds.views.events :as event]
            [birds.reports :as reports]))

(defn dispatch-global-update [key value notifications]
  (re-frame/dispatch [::event/update-bird-setting (-> key name keyword) value notifications]))

(defn dispatch-item-update [item key value notifications]
  (re-frame/dispatch [::event/update-observer-setting item key value notifications]))

(defn item-inputter [item input-type key desc & notifications]
  (input-type key desc
              (key item)
              #(dispatch-item-update item key % notifications)))

(defn inputter [input-type key desc propagate-to]
  (input-type key desc
              @(re-frame/subscribe [key])
              #(dispatch-global-update key % propagate-to)))

(defn num-input [key desc & propagate-to] (inputter html/float-input key desc propagate-to))
(defn int-input [key desc & propagate-to] (inputter html/int-input key desc propagate-to))
(defn prob-input [key desc & propagate-to] (inputter html/prob-input key desc propagate-to))
(defn ms-input [key desc & propagate-to] (inputter html/ms-input key desc propagate-to))
(defn checkbox [key desc & propagate-to] (inputter html/checkbox key desc propagate-to))

(defn gui []
  [:div {:class :controls}
   [int-input ::subs/num-of-birds "Number of birds: " ::event/generate-birds]
   [int-input ::subs/volume "Bird volume"]
   [int-input ::subs/audio-sensitivity "how far birds can hear"]
   [prob-input ::subs/spontaneous-sing-prob "Prob of singing in a given tick"]
   [prob-input ::subs/motivated-sing-prob "Prob of motivated singing"]
   [ms-input ::subs/motivated-sing-after "avg time to motivated singing [ticks]"]
   [ms-input ::subs/sing-rest-time "avg time of resting after singing [ticks]"]
   [ms-input ::subs/song-length "avg song length [tick]"]
   [num-input ::subs/speed "speed of simulation" ::event/set-speed]
   [ms-input ::subs/tick-length "tick length [ms]" ::event/set-tick-length]
   [checkbox ::subs/show-birds? "show bird location?"]
   [checkbox ::subs/show-bird-hear? "show bird hearing radius?"]
   [checkbox ::subs/show-observers? "show observer location?"]
   [checkbox ::subs/show-observer-hear? "show observer hearing radius?"]])

(defn strategy-selector [observer]
  (html/select :strategy "Observer strategy"
               (:strategy observer)
               @(re-frame/subscribe [::subs/observer-strategies])
               #(dispatch-item-update observer :strategy % nil)))

(defn observer-controls [observer]
  [:details {:key (gensym) :class :observer :open true}
   [:summary (:id observer)]
   [item-inputter observer html/checkbox :observing "Currently observing?" ::event/toggle-observation]
   [strategy-selector observer]
   [item-inputter observer html/int-input :audio-sensitivity "How far can the observer hear"]
   [item-inputter observer html/colour-picker :observer-colour "The colour of the observer"]
   [item-inputter observer html/colour-picker :hearing-colour "The colour of the hearing radius"]
   [:span {:class :observer-results} (str "Heard " (->> observer :observations (map :count) (reduce +)) " birds")]
   [:br]
   [:button {:on-click #(re-frame/dispatch [::event/remove-observer (:id observer)])} "Remove observer"]])

(defn observers []
  [:div {:class :observer-block}
   [:div {:class :observers}
    (map observer-controls @(re-frame/subscribe [::subs/observers]))]
   [:button {:on-click #(re-frame/dispatch [::event/add-observer])} "Add new observer"]])


(defn render-view []
  [:div
   [gui]
   [:hr]
   [observers]
   [:hr]
   (reports/show)])
