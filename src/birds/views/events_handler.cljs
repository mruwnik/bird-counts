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
            [birds.views.downloader :as downloader]
            [birds.views.events :as events]))

(defn assoc? [dict & vals]
  (reduce (fn [a [k v]] (if v (assoc a k v) a))
          dict
          (partition 2 vals)))

(defn event-last-param [_ event] (take-last 1 event))
(defn reg-side-effect-fx
  "Register an effect that will just call a function (for side effects).
  `param-extractor is used to extract any parameters from the event."
  ([key fun] (reg-side-effect-fx key fun nil))
  ([key fun param-extractor]
   (re-frame/reg-event-fx
    key
    (fn [context params]
      (if param-extractor
        (apply fun (param-extractor context params))
        (fun))
      nil))))

(defn reg-side-effect-db
  "Register an effect that needs the state map to cause a side effect.
  The state map won't be changed by this function - it's just to get side effects."
  [key fun] (re-frame/reg-event-db key (fn [db _] (fun db) db)))

(defn reg-conditional-db
  "Register an effect that will update the database with whatever the given `fun`
  returns, or if `nil` then the database will be left unchanged."
  [key fun] (re-frame/reg-event-db key (fn [db event] (or (fun db event) db))))

(defn reg-event-db-notifications
  "Register an effect that will update the database, and will also dispatch events
  to each item returned by `(apply propagate-to-fn event).
  It's assumed that the first item of the event is just a dispatch key, so will be ignored,
  and the last item is also ignored, coz I'm a lazy slob.
  So if an event like `[:event-id :param1 :param2 :param3 :notify-me]` is recieved,
  and `propagate-to-fn == last`, then an dispatch will be sent to `[:notify-me :param1 :param2 :param3]`"
  ([key propagate-to-fn fun] (reg-event-db-notifications key propagate-to-fn identity fun))
  ([key propagate-to-fn event-extract-fn fun]
   (re-frame/reg-event-fx
    key
    (fn [{db :db} event]
      (assoc?
       {:db (fun db event)}
       :fx (when-let [propagate-to (apply propagate-to-fn event)]
             (->> propagate-to
                  (map (partial conj (rest event)))
                  (map event-extract-fn)
                  (map vec)
                  (map (partial conj [:dispatch]))
                  vec)))))))

(defn reg-item-append-db
  "Register an effect that will append whatever is provided as the second arg of the event to `(db-key db).
  The optional `notifications` param can be provided for notifications to be dispatched."
  [key db-key & notifications]
  (reg-event-db-notifications key (constantly notifications) (fn [db [_ item]] (update db db-key conj item))))

(defn reg-item-replace-db
  "Register an effect that will remove whatever is provided as the second arg of the event from `(db-key db).
  The optional `notifications` param can be provided for notifications to be dispatched."
  [key db-key & notifications]
  (reg-event-db-notifications key (constantly notifications) (fn [db [_ item]] (update db db-key (partial remove #{item})))))

(defn reg-item-dissoc-db
  "Register an effect that will remove from `db` whatever is provided as the second value of the event.
  The optional `notifications` param can be provided for notifications to be dispatched."
  [key db-key & notifications]
  (reg-event-db-notifications key (constantly notifications) (fn [db [_ item]] (update db db-key dissoc item))))

(defn reg-item-assoc-db
  "Register an effect that will assoc-in `db` whatever is provided as the `(rest event)`.
  The optional `notifications` param can be provided for notifications to be dispatched."
  [key db-key & notifications]
  (reg-event-db-notifications key (constantly notifications)
     (fn [db event]
       (assoc-in db (->> event rest (drop-last 1) (concat [db-key])) (last event)))))

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
 (partial drop-last 1)
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
(reg-side-effect-fx ::events/set-tick-length time/set-tick-length! event-last-param)
(reg-side-effect-fx ::events/set-speed time/set-speed! event-last-param)

;; simulation events
(reg-side-effect-fx ::events/start-bird-loop sim-events/actors-loop)

(reg-side-effect-db ::events/start-render-forest forest/start-rendering)

(reg-conditional-db
 ::events/move-actor-by
 (fn [db [_ actor delta]]
   (when-let [item-key (condp = (str (type actor))
                    (str observers/Observer) :observers
                    (str birds/Bird)         :birds
                    nil)]
     (update-in db [item-key (:id actor)] actors/move-by! delta))))

;; Reporters
(reg-side-effect-fx ::events/initialize-reports reports/init!)
(reg-side-effect-fx ::events/attach-event-listener sim-events/attach-listener (partial take-last 1))

;; Observers
(reg-item-dissoc-db ::events/remove-observer :observers ::events/observer-removed)
(reg-item-append-db ::events/observer-added :observer-ids)
(reg-item-replace-db ::events/observer-removed :observer-ids)

(reg-side-effect-fx ::events/intitialise-observers-watch (fn [] (sim-events/attach-listener #(re-frame/dispatch [::events/observer-event %]))))

(re-frame/reg-event-fx
 ::events/add-observer
 (fn [{db :db} _]
   (let [new-ob (observers/new-observer db)]
     {:db (assoc-in db [:observers (:id new-ob)] new-ob)
      :dispatch [::events/observer-added (:id new-ob)]})))

(reg-event-db-notifications
 ::events/update-observer-setting
 (fn [_e _o _k _v propagate-to] propagate-to)
 (fn [db [_ observer-id key value]]
   (assoc-in db [:observers observer-id key] value)))

(re-frame/reg-event-db
 ::events/toggle-observation
 (fn [db [_ observer-id _ listen?]]
   (update-in db [:observers observer-id] (if listen? actors/start-listening actors/stop-listening))))

(defn inc-hearers [observers event]
  (->> observers
       vals
       (filter #(actors/hears? % event))
       (reduce #(update %1 (:id %2) actors/notice event) observers)))

(reg-conditional-db
 ::events/observer-event
 (fn [db [_ event]]
   (when (= (:event-type event) :start-singing)
     (update db :observers inc-hearers event))))


(reg-side-effect-fx
 ::events/download-observer-data
 (fn [file-type data] (downloader/download-data "events" file-type observers/observations-headers data))
 (fn [{db :db} [_ id file-type]] [file-type (-> db (get-in [:observers id]) observers/get-observations)]))

(re-frame/reg-event-db ::events/clear-observer-data (fn [db [_ id]] (update-in db [:observers id] observers/clear-observations)))
