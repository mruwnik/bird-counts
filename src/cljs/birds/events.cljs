(ns birds.events
  (:require [birds.time :as time]
            [birds.views.events :as event-types]
            [birds.simulate :as sim]
            [birds.views.subs :as subs]
            [re-frame.core :as re-frame]
            [cljs.core.async :as async]))

;; Reframe stuff

(def listeners (atom []))
(defn replace-bird [bird]
  (re-frame/dispatch [::event-types/update-bird bird]))
(defn dispatch-move [item]
  (re-frame/dispatch [::event-types/move-actor-by item]))
(defn dispatch-observer-update [observer]
  (re-frame/dispatch [::event-types/update-observer-setting (:id observer) :observations (:observations observer)]))

(defn attach-listener [func] (swap! listeners conj func))
(defn run-listeners [events]
  (doseq [fun @listeners event events]
    (fun event))
  events)

(defn run-tick [events]
  (time/tick!)
  (let [updates (sim/run-tick! @(re-frame/subscribe [::subs/observers])
                               @(re-frame/subscribe [::subs/birds])
                               events)]
    (doseq [bird (:updated-birds updates)] (replace-bird bird))
    (doseq [observer (:observers-spotted updates)] (dispatch-observer-update observer))
    (doseq [item (:moved updates)] (dispatch-move item))
    (run-listeners (:events updates))
    (:events updates)))

(defn actors-loop []
  (async/go-loop [events nil]
    (async/alts! [(async/timeout (time/next-tick-in))])
    (recur (if (= @(re-frame/subscribe [::subs/selected-tab]) :sandbox)
             (run-tick events) events))))
