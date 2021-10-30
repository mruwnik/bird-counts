(ns birds.events
  (:require [birds.bird :as bird]
            [birds.time :as time]
            [cljs.core.async :as async]))

(defn rand-happens? [prob] (< (rand 1) prob))
(defn same-bird? [{state :state} event] (= (:id @state) (:bird-id event)))

(def birds (atom []))
(def listeners (atom []))

(defn start-singing [{:keys [state] :as bird}]
  (bird/sing! bird)
  (-> @state
      (select-keys [:pos :volume :song-length])
      (assoc :event-type :start-singing
             :duration 0
             :bird-id (:id @state))))

(defn stop-singing [{:keys [state] :as bird}]
  (bird/stop-singing! bird)
  {:event-type :stop-singing :bird-id (:id @state)})

(defn resing [{:keys [state]}]
  {:event-type :re-sing
   :motivated-sing-in (:motivated-sing-after @state)
   :bird-id (:id @state)})

(defn handle-same-bird [{:keys [state] :as bird} {event-type :event-type :as event}]
  (condp = event-type
    :singing (if (> (:duration event) (:song-length event))
               (stop-singing bird)
               (update event :duration inc))

    :start-singing (-> event
                       (assoc :event-type :singing)
                       (update :duration inc))
    :re-sing (cond
               (> (:motivated-sing-in event) 0)
               (update event :motivated-sing-in dec)

               (bird/can-sing? bird)
               (do
                 (swap! state assoc :resinging true)
                 (assoc (start-singing bird) :motivated-singing true)))
    nil))

(defn handle-other-bird [{:keys [state] :as bird} {event-type :event-type :as event}]
  (condp = event-type
    nil (when (and (rand-happens? (:spontaneous-sing-prob @state))
                   (bird/can-sing? bird))
          (start-singing bird))

    :start-singing (when (and (bird/hears? bird event)
                              (bird/can-sing? bird)
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
      (bird/->Bird nil)))

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
