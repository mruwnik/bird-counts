(ns birds.views.events-handler
  (:require [re-frame.core :as re-frame]
            [birds.views.db :as db]
            [birds.actors :as actors]
            [birds.events :as sim-events]
            [birds.reports :as reports]
            [birds.forest :as forest]
            [birds.bird :as birds]
            [birds.time :as time]
            [birds.views.events :as events]))

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
 ::events/initialize-db
 (fn [_ _]
   (-> db/default-db :speed time/set-speed!)
   (-> db/default-db :tick-length time/set-tick-length!)
   db/default-db))

(re-frame/reg-event-db
 ::events/initialize-gui
 (fn [db _]
   (let [container (.getElementById js/document (:container-name db))
         width     (-> container .-clientWidth (- 40))]
     (assoc db
            :container container
            :width width
            :height (min width (-> js/document .-body .-clientHeight))))))

;; Birds
(re-frame/reg-event-fx
 ::events/update-bird-setting
 (fn [{db :db} [_ key value propagate-to]]
   (assoc?
    {:db (-> db
             (assoc key value)
             (update :birds (partial reduce-kv #(assoc %1 %2 (assoc %3 key value)) {})))}
    :fx (when (seq propagate-to)
          (vec (for [to propagate-to] [:dispatch [to key value]]))))))

(re-frame/reg-event-db
 ::events/generate-birds
 (fn [db _]
   (update db :birds birds/update-birds-count db)))


(re-frame/reg-event-db
 ::events/update-bird
 (fn [db [_ bird]]
   (assoc-in db [:birds (:id bird)] bird)))

;; Simulation controls
(reg-side-effect-fx ::events/set-tick-length time/set-tick-length! (partial take-last 1))
(reg-side-effect-fx ::events/set-speed time/set-speed! (partial take-last 1))

;; simulation events
(reg-side-effect-fx ::events/start-bird-loop sim-events/bird-loop)

(reg-side-effect-db ::events/start-render-forest forest/start-rendering)

;; Reporters
(reg-side-effect-fx ::events/initialize-reports reports/init!)

;; Observers
(reg-item-append-db ::events/add-observer :observers)
(reg-side-effect-fx ::events/intitialise-observers-watch
                    (fn [] (sim-events/attach-listener #(re-frame/dispatch [::events/observer-event %]))))

(re-frame/reg-event-db
 ::events/update-observer-setting
 (fn [{db :db} [_ observer key value]]
   (swap! (:state observer) key value)
   (prn key value @(:state observer))
   db))



(re-frame/reg-event-db
 ::events/observer-event
 (fn [db [_ event]]
   (if (= (:event-type event) :start-singing)
     (update db :observers #(for [o %]
                              (if (actors/hears? o event)
                                (update o :state swap! update :observations inc)
                                o)))
     db)))
