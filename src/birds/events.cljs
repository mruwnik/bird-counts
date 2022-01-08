(ns birds.events
  (:require [birds.actors :as actors]
            [birds.bird :as bird]
            [birds.time :as time]
            [cljs.core.async :as async]))

(defn rand-happens? [prob] (< (rand 1) prob))
(defn same-bird? [{state :state} event] (= (:id @state) (:bird-id event)))

(def birds (atom []))
(def listeners (atom []))

(defn base-event [{:keys [state]} type]
  (-> @state
      (select-keys [:pos :volume :song-length])
      (assoc :bird-id (:id @state)
             :event-type type)))

(defn start-singing [bird]
  (actors/sing! bird)
  (assoc (base-event bird :start-singing) :duration 0))

(defn stop-singing [bird]
  (actors/stop-singing! bird)
  (base-event bird :stop-singing))

(defn resing [{:keys [state] :as bird}]
  (assoc (base-event bird :re-sing)
         :motivated-sing-in (:motivated-sing-after @state)))

(defn handle-same-bird [{:keys [state] :as bird} {:keys [event-type duration song-length] :as event}]
  (condp = event-type
    :singing (if (> duration song-length)
               (assoc (stop-singing bird) :duration duration)
               (update event :duration inc))

    :start-singing (-> event
                       (assoc :event-type :singing)
                       (update :duration inc))
    :re-sing (cond
               (> (:motivated-sing-in event) 0)
               (update event :motivated-sing-in dec)

               (actors/can-sing? bird)
               (do
                 (swap! state assoc :resinging true)
                 (assoc (start-singing bird) :motivated-singing true)))
    nil))

(defn handle-other-bird [{:keys [state] :as bird} {event-type :event-type :as event}]
  (condp = event-type
    nil (when (and (rand-happens? (:spontaneous-sing-prob @state))
                   (actors/can-sing? bird))
          (start-singing bird))

    :start-singing (when (and (actors/hears? bird event)
                              (actors/can-sing? bird)
                              (rand-happens? (:motivated-sing-prob @state)))
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

(defn add-random-bird [settings id]
  (-> settings
      (select-keys [:audio-sensitivity :spontaneous-sing-prob
                    :motivated-sing-prob :motivated-sing-after
                    :sing-rest-time :song-length :volume])
      (assoc :id id
             :pos {:x (rand-int (:width settings))
                   :y (rand-int (:height settings))})
      atom
      (bird/->Bird)))

(defn update-birds-count! [{:keys [num-of-birds] :as settings}]
  (swap! birds (fn [birds]
                 (cond
                   (> (count birds) num-of-birds) (take num-of-birds birds)
                   (< (count birds) num-of-birds) (->> num-of-birds
                                                       (range (count birds))
                                                       (map (partial add-random-bird settings))
                                                       (concat birds))
                   :else birds))))

(defn attach-listener [func] (swap! listeners conj func))
(defn run-listeners [events]
  (doseq [fun @listeners event events]
    (fun event))
  events)

(defn bird-loop []
  (async/go-loop [events nil]
    (async/alts! [(async/timeout (time/next-tick-in))])
    (time/tick!)
    (recur (->> @birds
                (map (partial handle-bird events))
                (remove nil?)
                run-listeners))))
