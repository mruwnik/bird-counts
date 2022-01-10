(ns birds.views.db
  (:require [birds.converters :as conv]))

(def default-db
  {:container-name ["forest" identity]
   :frame-rate     [30 conv/parse-int]
   :num-of-birds   [10 conv/parse-int]
   :volume         [25 conv/parse-int]
   :spontaneous-sing-prob [0.01 conv/parse-float] ;; in a given 0.1s
   :motivated-sing-prob   [0.9 conv/parse-float]
   :motivated-sing-after  [30 conv/parse-int]
   :sing-rest-time        [100 conv/parse-int]
   :song-length           [20 conv/parse-int]
   :audio-sensitivity     [50 conv/parse-int]
   :show-bird-hear?     [true conv/parse-bool]
   :show-birds?         [true conv/parse-bool]
   :show-observers?     [true conv/parse-bool]
   :show-observer-hear? [true conv/parse-bool]

   :bird-colour    [[0 0 0] conv/parse-colour]
   :song-colour    [[0 0 255] conv/parse-colour]
   :resing-colour  [[0 150 0] conv/parse-colour]
   :hearing-colour [[255 0 0] conv/parse-colour]
   :resting-colour [[150 0 0] conv/parse-colour]

   :observer-strategies [:no-movement :follow-singing :wander]

   :speed       [10 conv/parse-int]
   :tick-length [100 conv/parse-int]})

(defn default-vals [] (reduce-kv #(assoc %1 %2 (first %3)) {} default-db))

(defn load-db []
  (reduce-kv #(assoc %1 %2 ((-> %2 default-db second) %3))
             (default-vals)
             (conv/parse-url-params)))
