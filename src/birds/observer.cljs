(ns birds.observer
  (:require [re-frame.core :as re-frame]
            [birds.actors :as actors]
            [birds.time :as time]
            [birds.views.events :as event]
            [birds.views.html :as html]
            [birds.views.subs :as subs]))

(defrecord Observer [pos observer-radius id
                     audio-sensitivity
                     observing observations
                     hearing-colour observer-colour]
  actors/Actor
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
  (draw-actor! [{:keys [pos observer-radius observer-colour]}]
    (actors/draw-circle! pos observer-radius (or observer-colour [0 50 100]))))

(defn make-id [] (gensym))

(defn new-observer [settings]
  (map->Observer {:id (make-id)
                  :observing true
                  :observations [{:start (time/now) :count 0}]
                  :audio-sensitivity 100
                  :hearing-colour [100 100 100]
                  :observer-colour [0 50 100]
                  :pos {:x (rand-int (:width settings))
                        :y (rand-int (:height settings))}
                  :observer-radius 10
                  :strategy :no-movement}))
