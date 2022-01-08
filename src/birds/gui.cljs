(ns birds.gui
  (:require [re-frame.core :as re-frame]
            [birds.views.subs :as subs]
            [birds.views.events :as event]))

(defn dispatch-update [key value propagate]
  (re-frame/dispatch [::event/update-bird-setting (-> key name keyword) value propagate]))

(defn num-input
  ([key desc] (num-input key desc #(js/parseInt % 10) nil))
  ([key desc parser & propagate-to]
   [:div {:class [:setting-input key]}
    desc
    [:input {:type "number"
             :value @(re-frame/subscribe [key])
             :on-change #(dispatch-update key (-> % .-target .-value parser) propagate-to)}]]))

(defn int-input
  [key desc & propagate-to] (apply num-input key desc #(js/parseInt % 10) propagate-to))

(defn prob-input
  [key desc]
  [:div {:class [:setting-input key]}
   desc
   [:input {:type "number"
            :min "0"
            :max "1"
            :step "0.01"
            :value @(re-frame/subscribe [key])
            :on-change #(dispatch-update key (-> % .-target .-value js/parseFloat) nil)}]])


(defn ms-input
  [key desc & propagate-to]
  [:div {:class [:setting-input key]}
   desc
   [:input {:type "number"
            :min "0"
            :step "100"
            :value @(re-frame/subscribe [key])
            :on-change #(dispatch-update key (-> % .-target .-value js/parseFloat) propagate-to)}]])

(defn checkbox [key desc]
  [:div {:class [:setting-input key]}
   desc
   [:input {:type "checkbox"
            :checked @(re-frame/subscribe [key])
            :on-change #(dispatch-update key (-> % .-target .-checked) nil)}]])


(defn controls []
  [:div {:class :controls}
   [int-input ::subs/num-of-birds "Number of birds: " ::event/generate-birds]
   [int-input ::subs/volume "Bird volume"]
   [int-input ::subs/audio-sensitivity "how far birds can hear"]
   [prob-input ::subs/spontaneous-sing-prob "Prob of singing in a given tick"]
   [prob-input ::subs/motivated-sing-prob "Prob of motivated singing"]
   [ms-input ::subs/motivated-sing-after "avg time to motivated singing [ticks]"]
   [ms-input ::subs/sing-rest-time "avg time of resting after singing [ticks]"]
   [ms-input ::subs/song-length "avg song length [tick]"]
   [num-input ::subs/speed "speed of simulation" #(js/parseFloat % 10) ::event/set-speed]
   [ms-input ::subs/tick-length "tick length [ms]" ::event/set-tick-length]
   [checkbox ::subs/show-birds? "show bird location?"]
   [checkbox ::subs/show-bird-hear? "show bird hearing radius?"]])
