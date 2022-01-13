(ns birds.views.html
  (:require [birds.converters :refer [parse-int parse-float dehexify hexify-rgb]]))

(defn num-input
  [class desc value dispatcher parser params]
  (let [id (gensym)]
    [:div {:class [:setting-input class] :id id}
     [:label {:for id} desc]
      [:input (assoc params
                     :type "number"
                     :value value
                     :on-change #(-> % .-target .-value parser dispatcher))]]))

(defn int-input
  [class desc value dispatcher]
  (num-input class desc value dispatcher parse-int {}))

(defn pos-int-input
  [class desc value dispatcher]
  (num-input class desc value dispatcher parse-int {:min "0"}))

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
