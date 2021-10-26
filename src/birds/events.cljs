(ns birds.events
  (:require [birds.bird :as bird]
            [cljs.core.async :as async]))

(defn rand-happens? [prob] (< (rand 1) prob))
(defn same-bird? [{state :state} event] (= (:id @state) (:bird-id event)))

(def event-chan (async/chan))
(def broadcast-chan (async/mult event-chan))

(defn publish
  ([msg] (publish event-chan msg))
  ([chan msg] (async/put! chan msg)))

(defn publish-after [ms msg]
  (async/go
    (async/<! (async/timeout ms))
    (async/>! event-chan msg)))

(defn kill! [{:keys [state chan]}] (publish chan {:event-type :die :bird-id (:id @state)}))

(defn start-singing [{:keys [state] :as bird}]
  ;; only start singing if more than `sing-rest-time`s have past since last sung
  (publish (-> @state
               (select-keys [:pos :volume :song-length])
               (assoc :event-type :start-singing
                                 :bird-id (:id @state))))
  (bird/sing! bird)
  (publish-after (:song-length @state)
                 {:event-type :stop-singing
                  :bird-id (:id @state)}))

(defn handle-event [{:keys [state] :as bird} {event-type :event-type :as event}]
  (condp = event-type
    nil (when (and (rand-happens? (:spontaneous-sing-prob @state))
                   (bird/can-sing? bird))
          (start-singing bird))

    :stop-singing (when (same-bird? bird event)
                    (bird/stop-singing! bird))

    :start-singing (when (and (not (same-bird? bird event))
                              (bird/hears? bird event)
                              (bird/can-sing? bird)
                              (rand-happens? (:motivated-sing-prob @state)))
                     (publish-after (:motivated-sing-after @state) {:event-type :re-sing :bla (rand-int 10000) :bird-id (:id @state)}))
    :re-sing (when (and (same-bird? bird event)
                        (bird/can-sing? bird))
               (prn event)
               (swap! state assoc :resinging true)
               (start-singing bird))

    nil))


(defn bird-runner [bird]
  (async/go-loop []
    (let [[{:keys [event-type] :as event} _] (async/alts! [(:chan bird) (async/timeout 100)])]
      (handle-event bird event)
      (if (and (same-bird? bird event) (= event-type :die))
        (async/untap broadcast-chan (:chan bird))
        (recur)))))

(defn random-bird [chan settings id]
  (let [bird (bird/->Bird (atom {:id id
                                 :pos {:x (rand-int (:width settings))
                                       :y (rand-int (:height settings))}
                                 :volume 25
                                 :spontaneous-sing-prob 0.01 ;; in a given 0.1s
                                 :motivated-sing-prob 0.9
                                 :motivated-sing-after 3000
                                 :sing-rest-time 10000
                                 :song-length 2000
                                 :audio-sensitivity 50})
                          (async/tap chan (async/chan)))]
    (swap! (:state bird) assoc :runner (bird-runner bird))
    bird))

(defn attach-listener [func]
  (let [chan (async/tap broadcast-chan (async/chan))]
    (async/go-loop [event (async/<! chan)]
      (when event
        (func event)
        (recur (async/<! chan))))
    chan))
