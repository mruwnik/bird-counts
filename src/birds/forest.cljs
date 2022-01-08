(ns birds.forest
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [re-frame.core :as re-frame]
            [birds.views.subs :as subs]
            [birds.actors :as actors]))

(defn setup [settings]
  ; Set frame rate to 30 frames per second.
  (q/frame-rate (:frame-rate settings))
  ;; setup function returns initial state
  {:color 0
   :angle 0})

(defn current-mouse-pos [] {:x (q/mouse-x) :y (q/mouse-y)})
(defn delta-pos [p1 p2] [(- (:x p2) (:x p1)) (- (:y p2) (:y p1))])
(defn move-by [pos [delta-x delta-y]] (-> pos (update :x + delta-x) (update :y + delta-y)))

(defn clicked-observer [observers]
  (when (q/mouse-pressed?)
    (->> observers
         (sort-by (comp :id deref :state))
         (filter (fn [o]
                   (let [{:keys [observer-radius pos]} @(:state o)]
                     (> observer-radius (actors/dist-2d pos (current-mouse-pos))))))
         first)))

(defn handle-mouse [state]
 (let [prev-selected (:currently-selected state)
        currently-selected (if (and (q/mouse-pressed?) prev-selected)
                             prev-selected
                             (clicked-observer @(re-frame/subscribe [::subs/observers])))
        prev-pos (:current-pos state)
        current-pos (current-mouse-pos)]

    (when (some-> prev-selected (= currently-selected))
      (actors/move-by! currently-selected (delta-pos prev-pos current-pos)))

    (assoc state
           :prev-selected prev-selected
           :currently-selected currently-selected
           :mouse-pressed? (q/mouse-pressed?)
           :prev-pos prev-pos
           :current-pos current-pos)))

(defn update-state [state]
  ; Update sketch state by changing circle color and position.
  (-> state
      (assoc :birds @(re-frame/subscribe [::subs/birds])
             :observers @(re-frame/subscribe [::subs/observers])
             :show-birds? @(re-frame/subscribe [::subs/show-birds?])
             :show-bird-hear? @(re-frame/subscribe [::subs/show-bird-hear?])
             :show-observers? @(re-frame/subscribe [::subs/show-observers?])
             :show-observer-hear? @(re-frame/subscribe [::subs/show-observer-hear?])
             :color (mod (+ (:color state) 0.7) 255)
             :angle (+ (:angle state) 0.1))
      handle-mouse))

(defn draw-state [state]
  ; Clear the sketch by filling it with light-grey color.
  (q/background 240)

  (when (:show-bird-hear? state)
    (doseq [bird (:birds state)]
      (actors/draw-hearing! bird)))
  (doseq [bird (->> state :birds (filter actors/singing?))]
    (actors/draw-song! bird))
  (when (:show-birds? state)
    (doseq [bird (:birds state)]
      (actors/draw-actor! bird)))

  (doseq [observer (:observers state)]
    (when (:show-observer-hear? state)
      (actors/draw-hearing! observer))
    (when (:show-observers? state)
      (actors/draw-actor! observer))))


(defn bird-updater [key settings]
  (doseq [bird (:birds settings)]
    (swap! (:state bird) assoc key (key settings))))


(defn start-rendering [settings]
  (q/defsketch birds
    :host (:container-name settings)
    :size [(:width settings) (:height settings)]
    ;; setup function called only once, during sketch initialization.
    :setup #(setup settings)
    ;; update-state is called on each iteration before draw-state.
    :update update-state
    :draw draw-state
    ;; This sketch uses functional-mode middleware.
    ;; Check quil wiki for more info about middlewares and particularly
    ;; fun-mode.
    :middleware [m/fun-mode]))
