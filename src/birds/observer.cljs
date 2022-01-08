(ns birds.observer
  (:require [re-frame.core :as re-frame]
            [birds.actors :as actors]
            [birds.views.events :as event]
            [birds.views.html :as html]
            [birds.views.subs :as subs]))

(defrecord Observer [state]
  actors/Actor
  (move! [_ x y] (swap! state assoc :pos {:x x :y y}))
  (hears? [_ event]
    (> (+ (:audio-sensitivity @state) (:volume event))
       (actors/dist-2d (:pos @state) (:pos event))))

  actors/Draw-actor
  (draw-song! [_])
  (draw-hearing! [observer]
    (actors/draw-circle! (:pos @state)
                         (:audio-sensitivity @state)
                         (or (:hearing-colour @state) [100 100 100])))
  (draw-actor! [{:keys [observer-colour pos]}]
    (actors/draw-circle! (:pos @state)
                         (:observer-radius @state)
                         (or (:observer-colour @state) [0 50 100]))))

(defn make-id [] (gensym))

(defn new-observer []
  (->Observer (atom {:id (make-id)
                     :observing false
                     :audio-sensitivity 100
                     :hearing-colour [100 100 100]
                     :observer-colour [0 50 100]
                     :pos {:x 0 :y 0}
                     :observer-radius 10
                     :strategy nil
                     :observations 0})))

(defn dispatch-update [observer key value]
  (re-frame/dispatch [::event/update-observer-setting observer key value]))

(defn inputter [observer input-type key desc]
  (input-type key desc
              (-> observer :state deref key)
              #(dispatch-update observer key %)))

(defn strategy-selector [observer]
  (html/select :strategy "Observer strategy"
               (-> observer :state deref :strategy)
               [:asd :fwe :asd :qwd]
               #(dispatch-update observer :strategy %)))

(defn observer-controls [observer]
  [:details {:key (gensym) :class :observer :open true}
   [:summary (-> observer :state deref :id)]
   [inputter observer html/checkbox :observing "Currently observing?"]
   [strategy-selector observer]
   [inputter observer html/int-input :audio-sensitivity "How far can the observer hear"]
   [inputter observer html/colour-picker :observer-colour "The colour of the observer"]
   [inputter observer html/colour-picker :hearing-colour "The colour of the hearing radius"]
   [:span {:class :observer-results} (str "Heard " (-> observer :state deref :observations) " birds")]
   [:br]
   [:button {:on-click #(re-frame/dispatch [::event/remove-observer (new-observer)])} "Remove observer"]
   ])

(defn controls []
  [:div {:class :observer-block}
   [:div {:class :observers}
    (map observer-controls @(re-frame/subscribe [::subs/observers]))]
   [:button {:on-click #(re-frame/dispatch [::event/add-observer (new-observer)])} "Add new observer"]
   ])
