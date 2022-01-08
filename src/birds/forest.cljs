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

(defn update-state [state]
  ; Update sketch state by changing circle color and position.
  (assoc state
         :birds @(re-frame/subscribe [::subs/birds])
         :show-birds? @(re-frame/subscribe [::subs/show-birds?])
         :show-bird-hear? @(re-frame/subscribe [::subs/show-bird-hear?])
         :color (mod (+ (:color state) 0.7) 255)
         :angle (+ (:angle state) 0.1)))

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
      (actors/draw-actor! bird))))

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
