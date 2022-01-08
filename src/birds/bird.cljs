(ns birds.bird
  (:require [quil.core :as q]
            [birds.actors :as actors]
            [birds.time :as time]))

(defn dist-2d [p1 p2]
  (Math/sqrt (+ (Math/pow (- (:x p1) (:x p2)) 2)
                (Math/pow (- (:y p1) (:y p2)) 2))))

(defn draw-circle! [pos size colour]
  (apply q/fill colour)
  (q/no-stroke)
  (q/ellipse (:x pos) (:y pos) (* 2 size) (* 2 size)))

(defrecord Bird [state chan]
  actors/Actor
  (move! [bird x y] (swap! state assoc :pos {:x x :y y}))
  (hears? [bird event]
    (> (+ (:audio-sensitivity @state) (:volume event))
       (dist-2d (:pos @state) (:pos event))))

  actors/Singer
  (sing! [_] (swap! state assoc
                    :singing-time (time/now)
                    :singing? true))
  (stop-singing! [_] (swap! state dissoc :singing? :resinging))
  (can-sing? [_]
    (let [{:keys [singing-time sing-rest-time]} @state]
      (or (not singing-time)
          (> (- (time/now) singing-time) sing-rest-time))))
  (singing? [_] (:singing? @state))

  actors/Draw-actor
  (draw-song! [_] (draw-circle! (:pos @state) (:volume @state) (or (:song-colour @state) [0 0 255])))
  (draw-hearing! [bird]
    (draw-circle! (:pos @state)
                  (:audio-sensitivity @state)
                  (cond
                    (:resinging @state) [0 150 0]
                    (not (actors/can-sing? bird)) [150 0 0]
                    (:hearing-colour @state) (:hearing-colour @state)
                    :else [255 0 0])))
  (draw-actor! [{:keys [bird-colour pos]}] (draw-circle! (:pos @state) 5 (or (:bird-colour @state) [0 0 0]))))
