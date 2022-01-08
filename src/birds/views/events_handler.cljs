(ns birds.views.events-handler
  (:require [re-frame.core :as re-frame]
            [birds.views.db :as db]
            [birds.actors :as actors]
            [birds.events :as sim-events]
            [birds.reports :as reports]
            [birds.forest :as forest]
            [birds.observer :as observers]
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
(defn reg-item-dissoc-db [key db-key]
  (re-frame/reg-event-db key (fn [db [_ item]] (update db db-key dissoc item))))
(defn reg-item-assoc-db [key db-key]
  (re-frame/reg-event-db
   key
   (fn [db event]
     (assoc-in db (->> event rest (drop-last 1) (concat [db-key])) (last event)))))

(defn reg-event-db-notifications [key propagate-to-fn fun]
  (re-frame/reg-event-fx
   key
   (fn [{db :db} event]
     (assoc?
      {:db (fun db event)}
      :fx (when-let [propagate-to (apply propagate-to-fn event)]
            (->> propagate-to
                 (map (partial conj (->> event rest (drop-last 1))))
                 (map vec)
                 (map (partial conj [:dispatch]))
                 vec))))))

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
(reg-event-db-notifications
 ::events/update-bird-setting
 (fn [_ _k _v propagate-to] propagate-to)
 (fn [db [_ key value]]
   (-> db
       (assoc key value)
       (update :birds (partial reduce-kv #(assoc %1 %2 (assoc %3 key value)) {})))))

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
(reg-side-effect-fx ::events/attach-event-listener sim-events/attach-listener (partial take-last 1))

;; Observers
(reg-item-dissoc-db ::events/remove-observer :observers)

(reg-side-effect-fx ::events/intitialise-observers-watch
                    (fn [] (sim-events/attach-listener #(re-frame/dispatch [::events/observer-event %]))))

(re-frame/reg-event-db
 ::events/add-observer
 (fn [db _]
   (let [new-ob (observers/new-observer db)]
     (assoc-in db [:observers (:id new-ob)] new-ob))))

(reg-event-db-notifications
 ::events/update-observer-setting
 (fn [_e _o _k _v propagate-to] propagate-to)
 (fn [db [_ observer key value]]
   (assoc-in db [:observers (:id observer) key] value)))

(re-frame/reg-event-db
 ::events/toggle-observation
 (fn [db [_ o _ listen?]]
   (update-in db [:observers (:id o)] (if listen? actors/start-listening actors/stop-listening))))

(re-frame/reg-event-db
 ::events/move-observer-by
 (fn [db [_ observer delta]]
   (update-in db [:observers (:id observer)] actors/move-by! delta)))

(defn inc-hearers [observers event]
  (->> observers
       vals
       (filter #(actors/hears? % event))
       (reduce #(update %1 (:id %2) actors/notice event) observers)))

(re-frame/reg-event-db
 ::events/observer-event
 (fn [db [_ event]]
   (if (= (:event-type event) :start-singing)
     (update db :observers inc-hearers event)
     db)))
