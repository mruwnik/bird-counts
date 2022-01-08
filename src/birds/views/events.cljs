(ns birds.views.events
  (:require [re-frame.core :as re-frame]
            [birds.views.db :as db]
            [birds.events :as sim-events]
            [birds.reports :as reports]
            [birds.forest :as forest]
            [birds.time :as time]))

(defn assoc? [dict & vals]
  (reduce (fn [a [k v]] (if v (assoc a k v) a))
          dict
          (partition 2 vals)))

(defn reg-side-effect-fx
  ([key fun] (reg-side-effect-fx key fun nil))
  ([key fun param-extractor]
   (re-frame/reg-event-fx
    key
    (fn [_ params]
      (if param-extractor
        (apply fun (param-extractor params))
        (fun))
      nil))))

(defn reg-side-effect-db [key fun] (re-frame/reg-event-db key (fn [db _] (fun db) db)))

(defn reg-item-append-db [key db-key]
  (re-frame/reg-event-db key (fn [db [_ item]] (update db db-key conj item))))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::initialize-gui
 (fn [db _]
   (let [container (.getElementById js/document (:container-name db))
         width     (-> container .-clientWidth (- 40))]
     (assoc db
            :container container
            :width width
            :height (min width (-> js/document .-body .-clientHeight))))))

(re-frame/reg-event-fx
 ::update-bird-setting
 (fn [{db :db} [_ key value propagate-to]]
   (doseq [bird (:birds db)]
     (swap! (:state bird) assoc key value))
   (assoc?
    {:db (assoc db key value)}
    :fx (when (seq propagate-to)
          (vec (for [to propagate-to] [:dispatch [to key value]]))))))

(re-frame/reg-event-db
 ::generate-birds
 (fn [db _]
   (assoc db :birds (sim-events/update-birds-count! db))))

;; Simulation controls
(reg-side-effect-fx ::set-tick-length time/set-tick-length! (partial take-last 1))
(reg-side-effect-fx ::set-speed time/set-speed! (partial take-last 1))

;; simulation events
(reg-side-effect-fx ::start-bird-loop sim-events/bird-loop)

(reg-side-effect-db ::start-render-forest forest/start-rendering)

;; Reporters
(reg-side-effect-fx ::initialize-reports reports/init!)

;; Observers
(reg-item-append-db ::add-observer :observers)
