(ns birds.observer
  (:require [re-frame.core :as re-frame]
            [birds.actors :as actors]
            [birds.time :as time]
            [birds.views.events :as event]
            [birds.views.html :as html]
            [birds.views.subs :as subs]))

(defrecord Observer [pos observer-radius id
                     audio-sensitivity
                     observing observations
                     hearing-colour observer-colour]
  actors/Actor
  (move-to! [o x y] (assoc o :pos {:x x :y y}))
  (move-by! [{{:keys [x y]} :pos :as o} [dx dy]] (assoc o :pos {:x (+ x dx) :y (+ y dy)}))

  actors/Listener
  (hears? [o event]
    (and (:observing o)
         (> (+ (:audio-sensitivity o) (:volume event))
            (actors/dist-2d (:pos o) (:pos event)))))
  (notice [{periods :observations :as o} event]
    (update-in o [:observations (-> periods count dec) :count] inc))

  (start-listening [o]
    (if (-> o :observations last :end)
      (update o :observations conj {:start (time/now)})
      o))
  (stop-listening [{periods :observations :as o}]
    (if (-> periods last :end)
      o
      (assoc-in o [:observations (-> periods count dec) :end] (time/now))))

  actors/Draw-actor
  (draw-song! [_])
  (draw-hearing! [{:keys [pos audio-sensitivity hearing-colour observing]}]
    (when observing
      (actors/draw-circle! pos audio-sensitivity (or hearing-colour [100 100 100]))))
  (draw-actor! [{:keys [pos observer-radius observer-colour]}]
    (actors/draw-circle! pos observer-radius (or observer-colour [0 50 100]))))

(defn make-id [] (gensym))

(defn new-observer [settings]
  (map->Observer {:id (make-id)
                  :observing true
                  :observations [{:start (time/now) :count 0}]
                  :audio-sensitivity 100
                  :hearing-colour [100 100 100]
                  :observer-colour [0 50 100]
                  :pos {:x (rand-int (:width settings))
                        :y (rand-int (:height settings))}
                  :observer-radius 10
                  :strategy nil}))

(defn dispatch-update [observer key value notifications]
  (re-frame/dispatch [::event/update-observer-setting observer key value notifications]))

(defn inputter [observer input-type key desc & notifications]
  (input-type key desc
              (key observer)
              #(dispatch-update observer key % notifications)))

(defn strategy-selector [observer]
  (html/select :strategy "Observer strategy"
               (:strategy observer)
               [:asd :fwe :asd :qwd]
               #(dispatch-update observer :strategy % nil)))

(defn observer-controls [observer]
  [:details {:key (gensym) :class :observer :open true}
   [:summary (:id observer)]
   [inputter observer html/checkbox :observing "Currently observing?" ::event/toggle-observation]
   [strategy-selector observer]
   [inputter observer html/int-input :audio-sensitivity "How far can the observer hear"]
   [inputter observer html/colour-picker :observer-colour "The colour of the observer"]
   [inputter observer html/colour-picker :hearing-colour "The colour of the hearing radius"]
   [:span {:class :observer-results} (str "Heard " (->> observer :observations (map :count) (reduce +)) " birds")]
   [:br]
   [:button {:on-click #(re-frame/dispatch [::event/remove-observer (:id observer)])} "Remove observer"]])

(defn controls []
  [:div {:class :observer-block}
   [:div {:class :observers}
    (map observer-controls @(re-frame/subscribe [::subs/observers]))]
   [:button {:on-click #(re-frame/dispatch [::event/add-observer])} "Add new observer"]])
