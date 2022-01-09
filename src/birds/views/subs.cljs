(ns birds.views.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub ::birds (fn [db] (-> db :birds vals)))
(re-frame/reg-sub ::observers (fn [db] (-> db :observers vals)))
(re-frame/reg-sub ::observer-strategies (fn [db] (-> db :observer-strategies)))

(re-frame/reg-sub ::num-of-birds (fn [db] (:num-of-birds db)))
(re-frame/reg-sub ::volume (fn [db] (:volume db)))
(re-frame/reg-sub ::audio-sensitivity (fn [db] (:audio-sensitivity db)))
(re-frame/reg-sub ::spontaneous-sing-prob (fn [db] (:spontaneous-sing-prob db)))
(re-frame/reg-sub ::motivated-sing-prob (fn [db] (:motivated-sing-prob db)))
(re-frame/reg-sub ::motivated-sing-after (fn [db] (:motivated-sing-after db)))
(re-frame/reg-sub ::sing-rest-time (fn [db] (:sing-rest-time db)))
(re-frame/reg-sub ::song-length (fn [db] (:song-length db)))
(re-frame/reg-sub ::speed (fn [db] (:speed db)))
(re-frame/reg-sub ::tick-length (fn [db] (:tick-length db)))
(re-frame/reg-sub ::show-birds? (fn [db] (:show-birds? db)))
(re-frame/reg-sub ::show-bird-hear? (fn [db] (:show-bird-hear? db)))
(re-frame/reg-sub ::show-observers? (fn [db] (:show-observers? db)))
(re-frame/reg-sub ::show-observer-hear? (fn [db] (:show-observer-hear? db)))
