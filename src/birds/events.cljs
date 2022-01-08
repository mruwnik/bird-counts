(ns birds.events
  (:require [birds.actors :as actors]
            [birds.bird :as bird]
            [birds.time :as time]
            [birds.views.events :as event-types]
            [birds.views.subs :as subs]
            [re-frame.core :as re-frame]
            [cljs.core.async :as async]))

(defn rand-happens? [prob] (< (rand 1) prob))
(defn same-bird? [bird event] (= (:id bird) (:bird-id event)))

(def listeners (atom []))

(defn base-event [bird type]
  (-> bird
      (select-keys [:pos :volume :song-length])
      (assoc :bird-id (:id bird)
             :event-type type)))

(defn replace-bird [bird]
  (re-frame/dispatch [::event-types/update-bird bird]))

(defn start-singing! [bird]
  (replace-bird (actors/sing! bird))
  (assoc (base-event bird :start-singing) :duration 0))

(defn stop-singing! [bird]
  (replace-bird (actors/stop-singing! bird))
  (base-event bird :stop-singing))

(defn start-resinging! [bird]
  (-> bird
      actors/resing!
      start-singing!
      (assoc :motivated-singing true)))

(defn resing [bird]
  (assoc (base-event bird :re-sing)
         :motivated-sing-in (:motivated-sing-after bird)))

(defn handle-same-bird [bird {:keys [event-type duration song-length] :as event}]
  (condp = event-type
    :singing (if (> duration song-length)
               (assoc (stop-singing! bird) :duration duration)
               (update event :duration inc))

    :start-singing (-> event
                       (assoc :event-type :singing)
                       (update :duration inc))
    :re-sing (cond
               (> (:motivated-sing-in event) 0)
               (update event :motivated-sing-in dec)

               (actors/can-sing? bird)
               (start-resinging! bird))
    nil))

(defn handle-other-bird [bird {event-type :event-type :as event}]
  (condp = event-type
    nil (when (and (rand-happens? (:spontaneous-sing-prob bird))
                   (actors/can-sing? bird))
          (start-singing! bird))

    :start-singing (when (and (actors/hears? bird event)
                              (actors/can-sing? bird)
                              (rand-happens? (:motivated-sing-prob bird)))
                     (resing bird))
    nil))

(defn handle-event [bird event]
  (if (same-bird? bird event)
    (handle-same-bird bird event)
    (handle-other-bird bird event)))

(defn handle-bird [events bird]
  (->> events
       (concat [nil])
       shuffle
       (map (partial handle-event bird))
       (remove nil?)
       first))

(defn attach-listener [func] (swap! listeners conj func))
(defn run-listeners [events]
  (doseq [fun @listeners event events]
    (fun event))
  events)

(defn bird-loop []
  (async/go-loop [events nil]
    (async/alts! [(async/timeout (time/next-tick-in))])
    (time/tick!)
    (recur (->> @(re-frame/subscribe [::subs/birds])
                (map (partial handle-bird events))
                (remove nil?)
                run-listeners
                doall))))
