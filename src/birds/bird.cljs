(ns birds.bird
  (:require [birds.actors :as actors]
            [birds.time :as time]))

(defrecord Bird [pos id actor-radius
                 volume audio-sensitivity
                 singing-time sing-rest-time
                 singing? resinging?
                 bird-colour song-colour hearing-colour]
  actors/Actor
  (actor-type [_] :bird)
  (move! [_])
  (move-to! [bird x y] (assoc bird :pos {:x x :y y}))
  (move-by! [{{:keys [x y]} :pos :as b} [dx dy]] (assoc b :pos {:x (+ x dx) :y (+ y dy)}))

  actors/Listener
  (hears? [{:keys [audio-sensitivity volume pos]} event]
    (> (+ audio-sensitivity volume)
       (actors/dist-2d pos (:pos event))))
  (notice [bird event])
  (start-listening [_])
  (stop-listening [_])

  actors/Singer
  (sing! [bird] (assoc bird :singing-time (time/now)
                            :singing? true))
  (resing! [bird] (assoc bird :resinging true))
  (stop-singing! [bird] (assoc bird :singing? false :resinging false))
  (can-sing? [{:keys [singing-time sing-rest-time]}]
    (or (not singing-time)
        (> (- (time/now) singing-time) sing-rest-time)))
  (singing? [bird] (:singing? bird))

  actors/Draw-actor
  (draw-song! [bird]
    (actors/draw-circle! (:pos bird) (:volume bird)
                         (or
                          (when (:resinging bird)
                            (:resing-colour bird))
                           (:song-colour bird)
                           [0 0 255])))
  (draw-hearing! [bird]
    (actors/draw-circle! (:pos bird) (:audio-sensitivity bird)
                         (or (when-not (actors/can-sing? bird)
                               (:resting-colour bird))
                             (:hearing-colour bird)
                             [255 0 0])))
  (draw-actor! [{:keys [bird-colour pos actor-radius]}]
    (actors/draw-circle! pos actor-radius (or bird-colour [0 0 0]))))

(defn add-random-bird [settings id]
  (-> settings
      (select-keys [:audio-sensitivity :spontaneous-sing-prob
                    :motivated-sing-prob :motivated-sing-after
                    :sing-rest-time :song-length :volume
                    :bird-colour :resing-colour :song-colour :hearing-colour :resting-colour])
      (assoc :id id :actor-radius 10
             :pos {:x (rand-int (:width settings))
                   :y (rand-int (:height settings))})
      (map->Bird)))

(defn update-birds-count [birds {:keys [num-of-birds] :as settings}]
  (cond
    (> (count birds) num-of-birds) (->> num-of-birds
                                        (range)
                                        (reduce #(assoc %1 %2 (birds %2)) {}))
    (< (count birds) num-of-birds) (->> num-of-birds
                                        (range (count birds))
                                        (reduce #(assoc %1 %2 (add-random-bird settings %2)) birds))
    :else birds))
