(ns birds.observer
  (:require [birds.actors :as actors]
            [birds.time :as time]))

(defn rand-bool [] (rand-nth [true false]))

(defn bound-val-delta [max-val current-val delta]
  (if (< 0 (+ current-val delta) max-val) delta 0))

(defn random-walk [{:keys [movement-speed pos patch-size]}]
  (let [{max-x :width max-y :height} patch-size
        delta-x (* (rand-nth [1 -1]) (rand-int movement-speed))
        delta-y (* (rand-nth [1 -1]) (rand-int movement-speed))]
    [(bound-val-delta max-x (:x pos) delta-x)
     (bound-val-delta max-y (:y pos) delta-y)]))

(defn wander [observer])

(defn follow-singing [{:keys [movement-speed pos local-state ignore-after] :as observer}]
  (let [{:keys [last-heard-pos last-heard-at]} @local-state]
    (cond
      ;; give up - it took too long
      (and last-heard-at (< (+ last-heard-at ignore-after) (time/now)))
      (do (swap! local-state dissoc :last-heard-at :last-heard-pos) nil)

      (and last-heard-pos (> (actors/dist-2d pos last-heard-pos) 10))
      (let [[delta-x delta-y] (actors/delta pos last-heard-pos)
            abs-delta-x (Math/abs delta-x)
            abs-delta-y (Math/abs delta-y)]
        ;; FIXME: This should really move proportionally to the distance...
        [(* (min abs-delta-x movement-speed) (if (zero? delta-x) 1 (/ delta-x abs-delta-x)))
         (* (min abs-delta-y movement-speed) (if (zero? delta-y) 1 (/ delta-y abs-delta-y)))])

      (:should-wander? observer)
      (wander observer)

      :else
      nil)))

(defrecord Observer [pos actor-radius id
                     audio-sensitivity
                     strategy movement-speed
                     observing observations
                     hearing-colour observer-colour]
  actors/Actor
  (move! [{:keys [strategy id] :as o}]
    (let [delta (case strategy
                  :no-movement nil
                  :random-walk (random-walk o)
                  :follow-singing (follow-singing o)
                  nil)]
      (when delta
        {:id id :delta delta})))
  (move-to! [o x y] (assoc o :pos {:x x :y y}))
  (move-by! [{{:keys [x y]} :pos :as o} [dx dy]] (assoc o :pos {:x (+ x dx) :y (+ y dy)}))

  actors/Listener
  (hears? [o event]
    (let [hears? (and (:observing o)
                      (> (+ (:audio-sensitivity o) (:volume event))
                         (actors/dist-2d (:pos o) (:pos event))))]
      (when (and hears?
                 (= (:strategy o) :follow-singing)
                 (not= (-> o :local-state deref :last-heard-pos) (:pos event)))
        (swap! (:local-state o) assoc :last-heard-pos (:pos event)
                                      :last-heard-at (time/now)))

      hears?))
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
                  :local-state (atom {})

                  :movement-speed 10 ; by how much the observer can move per tick
                  :ignore-after 100  ; stop following a specific bird after this many ticks
                  :should-wander? true
                  }))
