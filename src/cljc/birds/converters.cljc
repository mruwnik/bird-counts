(ns birds.converters
  (:require [clojure.string :as str]
            #?(:clj [clojure.edn :as edn])))

(defn parse-int [val] #?(:cljs (js/parseInt val 10)
                         :clj (edn/read-string val)))
(defn parse-hex [val] #?(:cljs (js/parseInt val 16)
                         :clj (Integer/parseInt val 16)))
(defn parse-float [val] #?(:cljs (js/parseFloat val 10)
                           :clj (edn/read-string val)))
(defn parse-bool [val] (-> val str/lower-case #{"1" "t" "true"}
                           #?(:cljs js/Boolean
                              :clj boolean)))
(defn parse-colour [val] (->> val (partition 2) (map (comp parse-hex str/join)) vec))
(defn param-range [bla] (prn bla))

(defn dehexify [val] (->> val rest parse-colour))
(defn hexify-rgb [[r g b]]
  (str "#"
       (-> (+ (bit-shift-left 1 24)
              (bit-shift-left r 16)
              (bit-shift-left g 8)
              (bit-shift-left b 0))
           (.toString 16)
           (subs 1))))

#?(:cljs
   (defn parse-url-params []
     (let [param-strs (-> js/window.location.search
                          (subs 1)
                          (str/split #"\&"))]
       (->> param-strs
            (map #(str/split % "="))
            (reduce #(assoc %1 (-> %2 first keyword) (second %2)) {})))))

(defn parse-values [parsers defaults to-parse]
  (reduce-kv #(assoc %1 %2 ((parsers %2) %3))
             defaults
             (select-keys to-parse (keys parsers))))

(defn event->data-point [event]
  (-> event
      (select-keys [:bird-id :tick :event-type :volume :song-length])
      (assoc :pos-x (-> event :pos :x)
             :pos-y (-> event :pos :x)
             :motivated (if (-> event :motivated-singing) 1 0))))

(defn seq->id-map [items] (reduce #(assoc %1 (:id %2) %2) {} items))
