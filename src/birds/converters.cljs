(ns birds.converters
  (:require [clojure.string :as str]))

(defn parse-int [val] (js/parseInt val 10))
(defn parse-hex [val] (js/parseInt val 16))
(defn parse-float [val] (js/parseFloat val 10) )
(defn parse-bool [val] (-> val str/lower-case #{"1" "t" "true"} js/Boolean))
(defn parse-colour [val] (->> val (partition 2) (map (comp parse-hex str/join)) vec))

(defn dehexify [val] (->> val rest parse-colour))
(defn hexify-rgb [[r g b]]
  (str "#"
       (-> (+ (bit-shift-left 1 24)
              (bit-shift-left r 16)
              (bit-shift-left g 8)
              (bit-shift-left b 0))
           (.toString 16)
           (subs 1))))

(defn parse-url-params []
  (let [param-strs (-> js/window.location.search
                       (subs 1)
                       (str/split #"\&"))]
    (->> param-strs
         (map #(str/split % "="))
         (reduce #(assoc %1 (-> %2 first keyword) (second %2)) {}))))

(defn parse-values [parsers defaults to-parse]
  (reduce-kv #(assoc %1 %2 ((parsers %2) %3))
             defaults
             (select-keys to-parse (keys parsers))))
