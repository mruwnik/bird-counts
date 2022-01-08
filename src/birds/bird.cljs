(ns birds.bird
  (:require [birds.actors :as actors]
            [birds.time :as time]))

(defrecord Bird [pos
                 volume audio-sensitivity
                 singing-time sing-rest-time
                 singing? resinging?
                 bird-colour song-colour hearing-colour]
  actors/Actor
  (move-to! [bird x y] (assoc bird :pos {:x x :y y}))
  (move-by! [_ [x y]])
  (hears? [{:keys [audio-sensitivity volume pos]} event]
    (> (+ audio-sensitivity volume)
       (actors/dist-2d pos (:pos event))))

  actors/Singer
  (sing! [bird] (assoc bird :singing-time (time/now)
                       :singing? true))
  (resing! [bird] (assoc bird :resinging? true))
  (stop-singing! [bird] (assoc bird :singing? false :resinging false))
  (can-sing? [{:keys [singing-time sing-rest-time]}]
    (or (not singing-time)
        (> (- (time/now) singing-time) sing-rest-time)))
  (singing? [bird] (:singing? bird))

  actors/Draw-actor
  (draw-song! [bird] (actors/draw-circle! (:pos bird) (:volume bird) (or (:song-colour bird) [0 0 255])))
  (draw-hearing! [bird]
    (actors/draw-circle! (:pos bird)
                  (:audio-sensitivity bird)
                  (cond
                    (:resinging bird) [0 150 0]
                    (not (actors/can-sing? bird)) [150 0 0]
                    (:hearing-colour bird) (:hearing-colour bird)
                    :else [255 0 0])))
  (draw-actor! [{:keys [bird-colour pos]}] (actors/draw-circle! pos 5 (or bird-colour [0 0 0]))))

(defn add-random-bird [settings id]
  (-> settings
      (select-keys [:audio-sensitivity :spontaneous-sing-prob
                    :motivated-sing-prob :motivated-sing-after
                    :sing-rest-time :song-length :volume])
      (assoc :id id
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
