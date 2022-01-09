(ns birds.views.html
  (:require [clojure.string :as str]))

(defn parse-int [val] (js/parseInt val 10))
(defn parse-hex [val] (js/parseInt val 16))
(defn parse-float [val] (js/parseFloat val 10) )

(defn num-input
  [class desc value dispatcher parser params]
  [:div {:class [:setting-input class]}
   desc
   [:input (assoc params
                  :type "number"
                  :value value
                  :on-change #(-> % .-target .-value parser dispatcher))]])

(defn int-input
  [class desc value dispatcher]
  (num-input class desc value dispatcher parse-int {}))

(defn float-input
  [class desc value dispatcher]
  (num-input class desc value dispatcher parse-float {}))

(defn prob-input
  [class desc value dispatcher]
  (num-input class desc value dispatcher parse-float
             {:min "0"
              :max "1"
              :step "0.01"}))

(defn ms-input
  [class desc value dispatcher]
  (num-input class desc value dispatcher parse-float
             {:min "0"
              :step "100"}))

(defn checkbox [class desc value dispatcher]
  [:div {:class [:setting-input class]}
   desc
   [:input {:type "checkbox"
            :checked value
            :on-change #(-> % .-target .-checked dispatcher)}]])

(defn dehexify [val] (->> val rest (partition 2) (map (comp parse-hex str/join)) vec))
(defn hexify-rgb [[r g b]]
  (str "#"
       (-> (+ (bit-shift-left 1 24)
              (bit-shift-left r 16)
              (bit-shift-left g 8)
              (bit-shift-left b 0))
           (.toString 16)
           (subs 1))))

(defn colour-picker [class desc value dispatcher]
  [:div {:class [:setting-input class]}
   desc
   [:input {:type :color :id (gensym)
            :value (hexify-rgb value)
            :on-change #(-> % .-target .-value dehexify dispatcher)}]])

(defn select [class desc value options dispatcher]
  [:div {:class [:setting-input class]}
   desc
   [:select {:name class :default-value value :on-change #(-> % .-target .-value dispatcher)}
    (when-not (seq (filter #{value} options))
      [:option {:value nil} "-"])
    (for [option options] [:option {:value option :key (gensym)} option])]])
