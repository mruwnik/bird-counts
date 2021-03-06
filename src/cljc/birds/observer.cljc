(ns birds.observer
  (:require [birds.actors :as actors]
            [birds.converters :as conv]
            [birds.time :as time]))

(defn rand-bool [] (rand-nth [true false]))

(defn bound-val-delta [max-val current-val delta]
  (if (< 0 (+ current-val delta) max-val) delta 0))

(defn bound-delta [bounds current [delta-x delta-y]]
  [(bound-val-delta (:width bounds) (:x current) delta-x)
   (bound-val-delta (:height bounds) (:y current) delta-y)])

(defn wander [{:keys [prob-change-direction local-state movement-speed patch-size pos]}]
  (when (< (rand) prob-change-direction)
    (let [delta-x (* (rand-nth [1 -1]) (rand-int movement-speed))
          delta-y (* (rand-nth [1 -1]) (- movement-speed (Math/abs delta-x)))]
      (swap! local-state assoc :wander-dir [delta-x delta-y])))
  (bound-delta patch-size pos (:wander-dir @local-state)))

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
  (actor-type [_] :observer)
  (move! [{:keys [strategy id] :as o}]
    (let [delta (case strategy
                  :no-movement    nil
                  :follow-singing (follow-singing o)
                  :wander         (wander o)
                  nil)]
      (when delta
        {:id id :delta delta :type :observers})))
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
    (actors/draw-dot! pos actor-radius (or observer-colour [0 50 100]))))

(def ids (atom 0))
(defn make-id [] (swap! ids inc))

(defn new-observer [settings]
  (map->Observer {:id               (:id settings (make-id))
                  :observing        (:observing settings true)

                  :pos (:pos settings {:x (rand-int (:width settings))
                                       :y (rand-int (:height settings))})
                  :patch-size (select-keys settings [:width :height])

                  :local-state (atom {})
                  :observations [{:start (time/now) :count 0}]

                  :audio-sensitivity (:audio-sensitivity settings 100)
                  :actor-radius      (:actor-radius settings 10)
                  :hearing-colour    (:hearing-colour settings [100 100 100])
                  :observer-colour   (:observer-colour settings [0 50 100])

                  :strategy (:strategy settings :no-movement)

                  :movement-speed (:movement-speed settings 5)  ; by how much the observer can move per tick
                  :ignore-after   (:ignore-after settings 100)  ; stop following a specific bird after this many ticks
                  :should-wander? (:should-wander? settings true)
                  :prob-change-direction (:prob-change-direction settings 0.05)}))

(def param-parsers
  {:observer-strategy       keyword
   :observer-movement-speed conv/parse-int
   ;; follow
   :observer-ignore-after conv/parse-int
   ;; wander
   :observer-should-wander?        conv/parse-bool
   :observer-prob-change-direction conv/parse-float})

(defn make-n-observers [settings n]
  (for [_ (range n)] (new-observer settings)))

(defn make-observers-from-params [settings params]
  (when (:observers params)
    (for [_ (-> params :observers conv/parse-int range)]
      (->> params
           (conv/parse-values param-parsers {})
           (reduce-kv #(assoc %1 (-> %2 name (subs 9) keyword) %3) {}) ; strip the prepending 'observer-'
           (merge (new-observer settings))))))

(def observations-headers [:start :end :count])
(defn get-observations [observer] (:observations observer))
(defn clear-observations [observer] (assoc observer :observations [{:start (time/now)}]))
