(ns birds.views.events-handler
  (:require [re-frame.core :as re-frame]
            [birds.views.db :as db]
            [birds.actors :as actors]
            [birds.simulate :as sim]
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
   (let [db (db/load-db)]
     (-> db :speed time/set-speed!)
     (-> db :tick-length time/set-tick-length!)
     db)))

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
(reg-item-assoc-db ::events/select-tab :selected-tab)

;; simulation events
(reg-side-effect-fx ::events/start-bird-loop sim-events/actors-loop)

(reg-conditional-db
 ::events/move-actor-by
 (fn [db [_ {:keys [id type delta]}]]
   (when (and type id)
     (update-in db [type id] actors/move-by! delta))))

;; Reporters
(reg-side-effect-fx ::events/initialize-reports reports/init!)
(reg-side-effect-fx ::events/attach-event-listener sim-events/attach-listener event-last-param)

;; Observers
(reg-item-dissoc-db ::events/remove-observer :observers ::events/observer-removed)
(reg-item-append-db ::events/observer-added :observer-ids)
(reg-item-replace-db ::events/observer-removed :observer-ids)

(re-frame/reg-event-fx
 ::events/add-observer
 (fn [{db :db} _]
   (let [new-ob (observers/new-observer db)]
     {:db (assoc-in db [:observers (:id new-ob)] new-ob)
      :dispatch [::events/observer-added (:id new-ob)]})))

(re-frame/reg-event-fx
 ::events/generate-observers
 (fn [{db :db} [_ params]]
   (when-let [obs (observers/make-observers db params)]
     {:db (assoc db :observers (reduce #(assoc %1 (:id %2) %2) {} obs))
      :fx (for [o obs] [:dispatch [::events/observer-added (:id o)]])})))

(reg-event-db-notifications
 ::events/update-observer-setting
 (fn [_e _o _k _v propagate-to] propagate-to)
 (fn [db [_ observer-id key value]]
   (assoc-in db [:observers observer-id key] value)))

(re-frame/reg-event-db
 ::events/toggle-observation
 (fn [db [_ observer-id _ listen?]]
   (update-in db [:observers observer-id] (if listen? actors/start-listening actors/stop-listening))))

(reg-side-effect-fx
 ::events/download-observer-data
 (fn [file-type data] (downloader/download-data "events" file-type observers/observations-headers data))
 (fn [{db :db} [_ id file-type]] [file-type (-> db (get-in [:observers id]) observers/get-observations)]))

(re-frame/reg-event-db ::events/clear-observer-data (fn [db [_ id]] (update-in db [:observers id] observers/clear-observations)))

;; Simulations
(re-frame/reg-event-db
 ::events/simulation-variable-dissoc
 (fn [db [_ key]] (update-in db [:simulation-options :variables] dissoc key)))

(re-frame/reg-event-db
 ::events/simulation-option-update
 (fn [db [_ path val]] (assoc-in db (concat [:simulation-options] path) val)))

(defn values-range [[from to steps]]
  (if (or (= from to) (<= steps 1))
    [from]
    (for [i (range steps)] (+ from (* i (/ (- to from) (dec steps)))))))

(defn blowup [items key values]
  (cond
    (and (seq items) (seq values))
    (for [item items value values] (assoc item key value))

    (seq values)
    (for [value values] {key value})

    (seq items) items))

(re-frame/reg-event-fx
 ::events/start-simulations
 (fn [{db :db} _]
   (let [{:keys [times ticks variables]} (:simulation-options db)
         base-settings (-> db
                           (select-keys [:width :height
                                         :num-of-birds :volume :spontaneous-sing-prob
                                         :motivated-sing-prob :motivated-sing-after
                                         :sing-rest-time :song-length :audio-sensitivity])
                           (assoc :times times :ticks ticks))]
     (when (seq variables)
       {:db (dissoc db :simulation-runs)
        :dispatch-later (->> variables
                             (reduce-kv #(assoc %1 %2 (values-range %3)) {})
                             (reduce-kv blowup [base-settings])
                             (conj [::events/run-single-simulation (:observers db)])
                             (assoc {:ms 400} :dispatch ))}))))

(reg-event-db-notifications
 ::events/run-single-simulation
 (fn [event _ [_ & to-run]] (when to-run [event]))
 (fn [[event observers [_ & to-run]]] [event observers to-run])
 (fn [db [_ observers [{:keys [times ticks] :as settings} & _]]]
   (update db :simulation-runs concat
           (for [i (range times)]
             (-> (birds/update-birds-count {} settings)
                 (sim/simulate-n-ticks observers ticks)
                 (merge settings)
                 (assoc :run (inc i)))))))

(reg-side-effect-fx
 ::events/download-simulation-runs
 (fn [file-type columns data] (prn data)(downloader/download-data "events" file-type columns data))
 (fn [{db :db} [_ file-type]] [file-type
                              (-> db :simulation-options :variables keys (conj :run :songs :motivated))
                              (-> db :simulation-runs)]))
