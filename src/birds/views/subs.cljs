(ns birds.views.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub ::actors (fn [db] (concat (-> db :birds vals) (-> db :observers vals))))
(re-frame/reg-sub ::birds (fn [db] (-> db :birds vals)))
(re-frame/reg-sub ::observers (fn [db] (-> db :observers vals)))
(re-frame/reg-sub ::observer (fn [db [_ id]] (-> db :observers (get id))))
(re-frame/reg-sub ::observer-value (fn [db [_ id key]] (get-in db [:observers id key])))
(re-frame/reg-sub ::observer-ids (fn [db] (->> db :observer-ids)))
(re-frame/reg-sub ::observer-strategies (fn [db] (-> db :observer-strategies)))

(def bird-settings [:width :height :num-of-birds :volume :spontaneous-sing-prob
                    :motivated-sing-prob :motivated-sing-after :sing-rest-time :song-length :audio-sensitivity])
(re-frame/reg-sub ::bird-settings (fn [db] (select-keys db bird-settings)))

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

(re-frame/reg-sub ::bird-colour (fn [db] (:bird-colour db)))
(re-frame/reg-sub ::song-colour (fn [db] (:song-colour db)))
(re-frame/reg-sub ::resing-colour (fn [db] (:resing-colour db)))
(re-frame/reg-sub ::hearing-colour (fn [db] (:hearing-colour db)))
(re-frame/reg-sub ::resting-colour (fn [db] (:resting-colour db)))

(re-frame/reg-sub ::simulation-runs (fn [db] (:simulation-runs db)))
(re-frame/reg-sub ::simulation-variables (fn [db] (-> db :simulation-options :variables keys)))
(re-frame/reg-sub ::simulation-available-variables (fn [db] (-> db :simulation-options :variables
                                                               keys (concat [:width :height])
                                                               set (remove bird-settings))))
(re-frame/reg-sub ::simulation-option (fn [db [_ path]] (get-in db (concat [:simulation-options] path))))
(re-frame/reg-sub ::selected-tab (fn [db] (or (:selected-tab db) :sandbox)))
(re-frame/reg-sub ::forest-settings (fn [db] (select-keys db [:width :height :container-name])))
