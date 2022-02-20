(ns birds.simulate
  (:require [birds.actors :as actors]
            [birds.time :as time]))

(defn rand-happens? [prob] (< (rand 1) prob))
(defn same-bird? [bird event] (= (:id bird) (:bird-id event)))

(defn base-event [bird type & kwargs]
  (-> bird
      (select-keys [:pos :volume :song-length])
      (assoc :bird-id (:id bird)
             :bird    bird
             :event-type type)
      (merge (apply hash-map kwargs))))

;; Handle bird actions

(defn start-singing [bird]
  (-> bird actors/sing! (base-event :start-singing :duration 0)))

(defn stop-singing [bird]
  (-> bird actors/stop-singing! (base-event :stop-singing :duration (:duration bird))))

(defn start-resinging [bird]
  (-> bird actors/resing! start-singing (assoc :motivated-singing true)))

(defn resing [bird]
  (base-event bird :re-sing :motivated-sing-in (:motivated-sing-after bird)))

(defn handle-same-bird [bird {:keys [event-type duration song-length] :as event}]
  (condp = event-type
    :singing (if (> duration song-length)
               (stop-singing bird)
               (update event :duration inc))

    :start-singing (-> event
                       (assoc :event-type :singing)
                       (update :duration inc))
    :re-sing (cond
               (> (:motivated-sing-in event) 0)
               (update event :motivated-sing-in dec)

               (actors/can-sing? bird)
               (start-resinging bird))
    nil))

(defn handle-other-bird [bird {event-type :event-type :as event}]
  (condp = event-type
    nil (when (and (rand-happens? (:spontaneous-sing-prob bird))
                   (actors/can-sing? bird))
          (start-singing bird))

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

;; Observers

(defn inc-hearers [events observer]
  (some->> events
       (filter (comp #{:start-singing} :event-type))
       (filter (partial actors/hears? observer))
       seq
       (reduce #(actors/notice %1 %2) observer)))

(defn updated-bird? [event]
  (or (= (:event-type event) :stop-singing)
      (and (:motivated-singing event) (= (:event-type event) :start-singing))
      (= (:event-type event) :start-singing)))

(defn run-tick! [observers birds prev-events]
  (let [events (->> birds
                    (map (partial handle-bird prev-events))
                    (remove nil?))]
    {:moved (->> (concat observers birds) (map actors/move!) (remove nil?))
     :observers-spotted (->> observers (map (partial inc-hearers events)) (remove nil?))
     :updated-birds (->> events (filter updated-bird?) (map :bird))
     :events (map #(dissoc % :bird) events)}))

;; Run simulations
(defn safe-add [& args] (->> args (remove nil?) (apply +)))

(defn update-stats [stats events]
  (let [songs (filter (comp #{:start-singing} :event-type) events)
        motivated (filter :motivated-singing songs)]
    (when (seq songs) songs)
    (-> stats
        (update :songs safe-add (count songs))
        (update :motivated safe-add (count motivated)))))

(defn reduce-with [acc fun items] (reduce fun acc items))
(defn merge-events [actors {:keys [updated-birds observers-spotted moved]}]
  (-> actors
      (reduce-with #(assoc-in %1 [:birds (:id %2)] %2) updated-birds)
      (reduce-with #(assoc-in %1 [:observers (:id %2)] %2) observers-spotted)
      (reduce-with #(update-in %1 [(:type %2) (:id %2)] actors/move-by! (:delta %2)) moved)))

(defn simulation-round [{:keys [birds observers stats last-events]}]
  ;; FIXME: birds assume that time flows. This is problematic...
  (time/tick!)
  (let [{new-events :events :as updates} (run-tick! (vals observers) (vals birds) last-events)]
    (-> {:birds birds :observers observers :last-events new-events}
        (merge-events updates)
        (assoc :stats (update-stats stats new-events)))))

(defn simulate-n-ticks [birds observers ticks]
  (with-redefs [time/clock (atom 0)]
    (let [{:keys [stats observers]} (->> {:birds birds :observers observers :stats {} :last-events []}
                                         (iterate simulation-round)
                                         (take ticks)
                                         last)]
      (->> observers
           (reduce-kv #(assoc %1 %2 (->> %3 :observations (map :count) (reduce +))) {})
           (assoc stats :observer-counts)))))


;; simulation helpers

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

(blowup nil :as [1 2 3 4])
