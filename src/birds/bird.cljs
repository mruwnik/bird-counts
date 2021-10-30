(ns birds.bird
  (:require [quil.core :as q]
            [birds.time :as time]))

(defn dist-2d [p1 p2]
  (Math/sqrt (+ (Math/pow (- (:x p1) (:x p2)) 2)
                (Math/pow (- (:y p1) (:y p2)) 2))))

(defprotocol Bird
  (move! [bird x y])
  (hears? [bird event]))

(defprotocol Singer
  (sing! [bird])
  (stop-singing! [bird])
  (can-sing? [bird])
  (singing? [bird]))

(defprotocol Draw-bird
  (draw-bird! [bird])
  (draw-song! [bird])
  (draw-hearing! [bird]))

(defn draw-circle! [pos size colour]
  (apply q/fill colour)
  (q/no-stroke)
  (q/ellipse (:x pos) (:y pos) (* 2 size) (* 2 size)))

(defrecord Bird [state chan]
  Bird
  (move! [bird x y] (swap! state assoc :pos {:x x :y y}))
  (hears? [bird event]
    (> (+ (:audio-sensitivity @state) (:volume event))
       (dist-2d (:pos @state) (:pos event))))

  Singer
  (sing! [_] (swap! state assoc
                    :singing-time (time/now)
                    :singing? true))
  (stop-singing! [_] (swap! state dissoc :singing? :resinging))
  (can-sing? [_]
    (let [{:keys [singing-time sing-rest-time]} @state]
      (or (not singing-time)
          (> (- (time/now) singing-time) sing-rest-time))))
  (singing? [_] (:singing? @state))

  Draw-bird
  (draw-song! [_] (draw-circle! (:pos @state) (:volume @state) (or (:song-colour @state) [0 0 255])))
  (draw-hearing! [bird]
    (draw-circle! (:pos @state)
                  (:audio-sensitivity @state)
                  (cond
                    (:resinging @state) [0 150 0]
                    (not (can-sing? bird)) [150 0 0]
                    (:hearing-colour @state) (:hearing-colour @state)
                    :else [255 0 0])))
  (draw-bird! [{:keys [bird-colour pos]}] (draw-circle! (:pos @state) 5 (or (:bird-colour @state) [0 0 0]))))
