(ns birds.views.html
  (:require [clojure.string :as str]))

(defn parse-int [val] (js/parseInt val 10))
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

(defn colour-picker [class desc value dispatcher]
  [:div {:class [:setting-input class]
         :on-click dispatcher}
   desc
   [:div {:style {:background-color (str "rgb(" (str/join ", " value) ")")}} "_"]])

(defn select [class desc value options dispatcher]
  [:div {:class [:setting-input class]}
   desc
   [:select {:name class :default-value value :on-select dispatcher}
    [:option {:value nil} "-"]
    (for [option options] [:option {:value option :key (gensym)} option])]])
