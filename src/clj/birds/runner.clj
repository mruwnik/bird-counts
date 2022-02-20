(ns birds.runner
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [birds.bird :as birds]
            [birds.observer :as observers]
            [birds.simulate :as sim]))

(defn write-csv-row
  ([file values]
   (.write file (str/join "," values))
   (.write file "\r\n")
   (.flush file))
  ([file headers item]
  (->> headers
       (map item)
       (map #(if-not (nil? %) (str %) ""))
       (write-csv-row file))))

(defn make-observers [settings]
  (->> settings
       :observers
       (map (partial merge (select-keys settings [:width :height])))
       (map observers/new-observer)
       (reduce #(assoc %1 (:id %2) %2) {})))

(defn run-simulation [settings variables]
  (let [settings (merge settings variables)]
    (-> (birds/update-birds-count {} settings)
        (sim/simulate-n-ticks (make-observers settings) (:ticks settings))
        (merge settings))))

(defn log-results [variables {:keys [log songs motivated] :as item}]
  (when log
    (->> variables
         keys
         (map #(str (name %) ": " (% item)))
         (str/join ", ")
         print)
    (println " -> spontaneous:" songs "motivated:" motivated))
  item)

(defn run-simulations! [settings output-file]
  (let [variables (-> settings :variables
                      (assoc :run (range (:times settings))))
        headers (concat (keys variables) [:songs :motivated])]
    (with-open [wtr (io/writer output-file)]
      (write-csv-row wtr (map name headers))
      (->> variables
           (reduce-kv sim/blowup [])
           (pmap (partial run-simulation settings))
           (map (partial log-results variables))
           (map (partial write-csv-row wtr headers))
           doall))
    "done"))
