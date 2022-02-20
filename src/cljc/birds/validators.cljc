(ns birds.validators
  (:require [birds.converters :as conv]))

(defn error-when-false [func error]
  (fn [val] (when-not (func val) error)))

(defn between [a b]
  (error-when-false #(when (number? %) (<= a % b)) (str "Value must be between " a " and " b)))

(defn multi-validate [& validators]
  (fn [val] (->> validators
                 (map #(% val))
                 (remove nil?)
                 seq)))

(defn- validate-into [validators acc k val]
  (or (when-let [validator (validators k)]
        (when-let [errors (validator val)]
          (assoc acc k errors)))
      acc))

(defn- nil-if-empty [coll] (when (seq coll) coll))
(defn map-validators [validators]
  (fn [val]
    (->> val (reduce-kv (partial validate-into validators) {}) nil-if-empty)))

(defn- val-num [validator] (fn [val] (when (number? val) (validator val))))
(def bool? (error-when-false #{true false} "Value must be a boolean"))
(def positive? (error-when-false (val-num pos?) "Value must be positive"))
(def non-negative? (error-when-false (val-num (complement neg?)) "Value must be positive"))
(def num? (error-when-false number? "Value must be an integer"))
(def is-int? (error-when-false int? "Value must be an integer"))
(def is-float? (error-when-false float? "Value must be a float"))
(def pos-float? (multi-validate is-float? positive?))
(def natural? (multi-validate is-int? non-negative?))
(def time-span? natural?)
(def probability? (multi-validate num? (between 0 1)))

(defn coords? [{:keys [x y]}]
  (cond-> []
    (not x) (conj "No x coordinate provided")
    (not y) (conj "No y coordinate provided")
    (and x (or (not (int? x)) (neg? x) )) (conj "x coordinates must be positive integer")
    (and y (or (not (int? y)) (neg? y))) (conj "y coordinates must be positive integer")
    true seq))

(defn html-colour? [vals]
  (when-not (every? #(< 0 % 255) vals) "Colour codes must consist of 3 integers between 0 and 255"))

(def all-observer-strategies #{:no-movement :follow-singing :wander})
(def observer-strategy?
  (error-when-false all-observer-strategies
                    (str "Unsupported observer strategy - use one of "
                         all-observer-strategies)))

(def observer-validators
  {:observing  bool?
   :pos        coords?

   :audio-sensitivity natural?
   :actor-radius      natural?
   :hearing-colour    html-colour?
   :observer-colour   html-colour?

   :strategy          observer-strategy?

   :movement-speed positive?
   :ignore-after   time-span?
   :should-wander? bool?
   :prob-change-direction probability?})

(defn validate-observers [observers]
  (let [validator (map-validators observer-validators)]
    (->> observers
         (map #(vector (:id %) (validator %)))
         (filter second)
         (into {})
         nil-if-empty)))

(def validators
  {:frame-rate            natural?
   :num-of-birds          natural?
   :volume                natural?
   :audio-sensitivity     natural?
   :spontaneous-sing-prob probability?
   :motivated-sing-prob   probability?
   :motivated-sing-after  time-span?
   :sing-rest-time        time-span?
   :song-length           time-span?

   :observer-strategies  observer-strategy?
   :observers            validate-observers

   :show-bird-hear?     bool?
   :show-birds?         bool?
   :show-observers?     bool?
   :show-observer-hear? bool?

   :bird-colour    html-colour?
   :song-colour    html-colour?
   :resing-colour  html-colour?
   :hearing-colour html-colour?
   :resting-colour html-colour?

   :speed       natural?
   :tick-length natural?})

(def validate-settings (map-validators validators))
