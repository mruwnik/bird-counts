(ns birds.time)

(def clock (atom 0))
(def speed (atom 1))
(def tick-length (atom 100))

(defn tick! [] (swap! clock inc))
(defn now [] @clock)

(defn set-speed! [new-speed] (reset! speed new-speed))
(defn set-tick-length! [length] (reset! tick-length length))
(defn next-tick-in
  "Returns when the next tick will be in milliseconds" []
  (/ @tick-length @speed))
