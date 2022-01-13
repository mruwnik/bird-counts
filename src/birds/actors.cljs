(ns birds.actors
  (:require [quil.core :as q]))

(defn dist-2d [p1 p2]
  (Math/sqrt (+ (Math/pow (- (:x p1) (:x p2)) 2)
                (Math/pow (- (:y p1) (:y p2)) 2))))

(defn delta [p1 p2] [(- (:x p2) (:x p1)) (- (:y p2) (:y p1))])

(defn draw-circle! [pos size colour]
  (apply q/fill colour)
  (q/no-stroke)
  (q/ellipse (:x pos) (:y pos) (* 2 size) (* 2 size)))

(defprotocol Actor
  (actor-type [_])
  (move! [_])
  (move-to! [_ x y])
  (move-by! [_ [x y]]))

(defprotocol Listener
  (hears? [_ event])
  (notice [_ event])
  (start-listening [_])
  (stop-listening [_]))

(defprotocol Singer
  (sing! [_])
  (resing! [_])
  (stop-singing! [_])
  (can-sing? [_])
  (singing? [_]))

(defprotocol Draw-actor
  (draw-actor! [actor])
  (draw-song! [actor])
  (draw-hearing! [actor]))
