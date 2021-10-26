(ns birds.forest
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [birds.bird :as bird]))

(defn setup [settings]
  ; Set frame rate to 30 frames per second.
  (q/frame-rate (:frame-rate @settings))
  ;; setup function returns initial state
  {:settings settings
   :color 0
   :angle 0})

(defn update-state [state]
  ; Update sketch state by changing circle color and position.
  (assoc state
         :birds (-> state :settings deref :birds)
         :show-birds? (-> state :settings deref :show-birds?)
         :show-bird-hear? (-> state :settings deref :show-bird-hear?)
         :color (mod (+ (:color state) 0.7) 255)
         :angle (+ (:angle state) 0.1)))

(defn draw-state [state]
  ; Clear the sketch by filling it with light-grey color.
  (q/background 240)
  ; Set circle color.
  ;; (q/fill (:color state) 255 255)

  (when (:show-bird-hear? state)
    (doseq [bird (:birds state)]
      (bird/draw-hearing! bird)))
  (doseq [bird (->> state :birds (filter bird/singing?))]
    (bird/draw-song! bird))
  (when (:show-birds? state)
    (doseq [bird (:birds state)]
      (bird/draw-bird! bird))))

(defn bird-updater [key settings]
  (doseq [bird (:birds settings)]
    (swap! (:state bird) assoc key (key settings))))


(defn start-rendering [settings]
  (q/defsketch birds
    :host (:container-name @settings)
    :size [(:width @settings) (:height @settings)]
    ;; setup function called only once, during sketch initialization.
    :setup #(setup settings)
    ;; update-state is called on each iteration before draw-state.
    :update update-state
    :draw draw-state
    ;; This sketch uses functional-mode middleware.
    ;; Check quil wiki for more info about middlewares and particularly
    ;; fun-mode.
    :middleware [m/fun-mode]))
