(ns birds.actors
  (:require [quil.core :as q]))

(defn dist-2d [p1 p2]
  (Math/sqrt (+ (Math/pow (- (:x p1) (:x p2)) 2)
                (Math/pow (- (:y p1) (:y p2)) 2))))

(defn draw-circle! [pos size colour]
  (apply q/fill colour)
  (q/no-stroke)
  (q/ellipse (:x pos) (:y pos) (* 2 size) (* 2 size)))

(defprotocol Actor
  (move-to! [_ x y])
  (move-by! [_ [x y]])
  (hears? [bird event]))

(defprotocol Singer
  (sing! [bird])
  (resing! [bird])
  (stop-singing! [bird])
  (can-sing? [bird])
  (singing? [bird]))

(defprotocol Draw-actor
  (draw-actor! [actor])
  (draw-song! [actor])
  (draw-hearing! [actor]))
