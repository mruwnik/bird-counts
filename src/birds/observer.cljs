(ns birds.observer
  (:require [birds.actors :as actors]
            [birds.time :as time]))

(defn rand-bool [] (rand-nth [true false]))

(defn bound-val-delta [max-val current-val delta]
  (if (< 0 (+ current-val delta) max-val) delta 0))

(defn random-walk [{:keys [id movement-speed pos patch-size]}]
  (let [{max-x :width max-y :height} patch-size
        delta-x (* (rand-nth [1 -1]) (rand-int movement-speed))
        delta-y (* (rand-nth [1 -1]) (rand-int movement-speed))]
    {:id id :delta [(bound-val-delta max-x (:x pos) delta-x)
                    (bound-val-delta max-y (:y pos) delta-y)]}))

(defrecord Observer [pos actor-radius id
                     audio-sensitivity
                     strategy movement-speed
                     observing observations
                     hearing-colour observer-colour]
  actors/Actor
  (move! [{:keys [strategy id movement-speed] :as o}]
    (case strategy
      :no-movement nil
      :random-walk (random-walk o)
      nil))
  (move-to! [o x y] (assoc o :pos {:x x :y y}))
  (move-by! [{{:keys [x y]} :pos :as o} [dx dy]] (assoc o :pos {:x (+ x dx) :y (+ y dy)}))

  actors/Listener
  (hears? [o event]
    (and (:observing o)
         (> (+ (:audio-sensitivity o) (:volume event))
            (actors/dist-2d (:pos o) (:pos event)))))
  (notice [{periods :observations :as o} event]
    (update-in o [:observations (-> periods count dec) :count] inc))

  (start-listening [o]
    (if (-> o :observations last :end)
      (update o :observations conj {:start (time/now)})
      o))
  (stop-listening [{periods :observations :as o}]
    (if (-> periods last :end)
      o
      (assoc-in o [:observations (-> periods count dec) :end] (time/now))))

  actors/Draw-actor
  (draw-song! [_])
  (draw-hearing! [{:keys [pos audio-sensitivity hearing-colour observing]}]
    (when observing
      (actors/draw-circle! pos audio-sensitivity (or hearing-colour [100 100 100]))))
  (draw-actor! [{:keys [pos actor-radius observer-colour]}]
    (actors/draw-circle! pos actor-radius (or observer-colour [0 50 100]))))

(def ids (atom 0))
(defn make-id [] (swap! ids inc))

(defn new-observer [settings]
  (map->Observer {:id (make-id)
                  :observing true
                  :observations [{:start (time/now) :count 0}]
                  :audio-sensitivity 100
                  :hearing-colour [100 100 100]
                  :observer-colour [0 50 100]
                  :pos {:x (rand-int (:width settings))
                        :y (rand-int (:height settings))}
                  :patch-size (select-keys settings [:width :height])
                  :actor-radius 10

                  :strategy :no-movement
                  :movement-speed 10}))
