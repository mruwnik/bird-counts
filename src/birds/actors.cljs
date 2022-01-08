(ns birds.actors)

(defprotocol Actor
  (move! [bird x y])
  (hears? [bird event]))

(defprotocol Singer
  (sing! [bird])
  (stop-singing! [bird])
  (can-sing? [bird])
  (singing? [bird]))

(defprotocol Draw-actor
  (draw-actor! [actor])
  (draw-song! [actor])
  (draw-hearing! [actor]))
