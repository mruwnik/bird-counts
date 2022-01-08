(ns birds.gui
  (:require [re-frame.core :as re-frame]
            [birds.views.subs :as subs]
            [birds.views.html :as html]
            [birds.views.events :as event]))

(defn dispatch-update [key value propagate]
  (re-frame/dispatch [::event/update-bird-setting (-> key name keyword) value propagate]))

(defn inputter [input-type key desc propagate-to]
  (input-type key desc
              @(re-frame/subscribe [key])
              #(dispatch-update key % propagate-to)))

(defn num-input [key desc & propagate-to] (inputter html/float-input key desc propagate-to))
(defn int-input [key desc & propagate-to] (inputter html/int-input key desc propagate-to))
(defn prob-input [key desc & propagate-to] (inputter html/prob-input key desc propagate-to))
(defn ms-input [key desc & propagate-to] (inputter html/ms-input key desc propagate-to))
(defn checkbox [key desc & propagate-to] (inputter html/checkbox key desc propagate-to))

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
   [num-input ::subs/speed "speed of simulation" ::event/set-speed]
   [ms-input ::subs/tick-length "tick length [ms]" ::event/set-tick-length]
   [checkbox ::subs/show-birds? "show bird location?"]
   [checkbox ::subs/show-bird-hear? "show bird hearing radius?"]
   [checkbox ::subs/show-observers? "show observer location?"]
   [checkbox ::subs/show-observer-hear? "show observer hearing radius?"]])
