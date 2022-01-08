(ns birds.bird
  (:require [birds.actors :as actors]
            [birds.time :as time]))

(defrecord Bird [state]
  actors/Actor
  (move! [bird x y] (swap! state assoc :pos {:x x :y y}))
  (hears? [bird event]
    (> (+ (:audio-sensitivity @state) (:volume event))
       (actors/dist-2d (:pos @state) (:pos event))))

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
  (draw-song! [_] (actors/draw-circle! (:pos @state) (:volume @state) (or (:song-colour @state) [0 0 255])))
  (draw-hearing! [bird]
    (actors/draw-circle! (:pos @state)
                  (:audio-sensitivity @state)
                  (cond
                    (:resinging @state) [0 150 0]
                    (not (actors/can-sing? bird)) [150 0 0]
                    (:hearing-colour @state) (:hearing-colour @state)
                    :else [255 0 0])))
  (draw-actor! [{:keys [bird-colour pos]}] (actors/draw-circle! (:pos @state) 5 (or (:bird-colour @state) [0 0 0]))))
