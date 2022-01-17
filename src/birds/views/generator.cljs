(ns birds.views.generator
  (:require [re-frame.core :as re-frame]
            [birds.simulate :as sim]
            [birds.views.subs :as subs]
            [birds.views.events :as events]
            [birds.views.html :as html]
            [birds.bird :as birds]))

(defn simulate-for-settings [settings ticks observers]
  (sim/simulate-n-ticks (birds/update-birds-count {} settings) observers ticks))

(def input-types
  {:num-of-birds          html/int-input
   :volume                html/int-input
   :spontaneous-sing-prob html/prob-input
   :motivated-sing-prob   html/prob-input
   :motivated-sing-after  html/ms-input
   :sing-rest-time        html/ms-input
   :song-length           html/ms-input
   :audio-sensitivity     html/int-input})

(defn dispatch-sim-option [path]
  (fn [val] (re-frame/dispatch [::events/simulation-option-update path val])))

(defn setting-input [html-type key desc path]
  [html-type key desc @(re-frame/subscribe [::subs/simulation-option path]) (dispatch-sim-option path)])

(defn settings []
  [:div {:class :settings}
   [:button {:class :run-simulations :on-click #(re-frame/dispatch [::events/start-simulations])} "Run simulations"]
   [:br]
   [:button {:class :run-simulations :on-click #(re-frame/dispatch [::events/download-simulation-runs :json])} "json"]
   [:button {:class :run-simulations :on-click #(re-frame/dispatch [::events/download-simulation-runs :csv])} "csv"]
   [:div {:class :simulation-length}
    [setting-input html/int-input :ticks "how many ticks per simulation run" [:ticks]]
    [setting-input html/int-input :times "how many times to run simulation per variable set" [:times]]]
   (html/select :simulation-options "Variables" nil
                @(re-frame/subscribe [::subs/simulation-available-variables])
                (fn [val] (let [key (keyword val)
                               dispatcher (dispatch-sim-option [:variables key])
                               current-val (key @(re-frame/subscribe [::subs/bird-settings]))]
                           (dispatcher [current-val current-val 0]))))
   (for [key @(re-frame/subscribe [::subs/simulation-variables])]
     [:div {:class :generator-option :key (gensym)}
      [:label key]
      [setting-input (input-types key) key "from" [:variables key 0]]
      [setting-input (input-types key) key "to" [:variables key 1]]
      [setting-input html/pos-int-input key "step" [:variables key 2]]
      [:button {:class [:remove-option key]
                :on-click #(re-frame/dispatch [::events/simulation-variable-dissoc key])} "X"]])])

(defn results []
  [:table {:class :results}
   [:thead
    [:tr {:class :result-header :key (gensym)}
     (for [dimension @(re-frame/subscribe [::subs/simulation-variables])]
       [:th {:class [dimension :dimension] :key (gensym)} (name dimension)])

     [:th {:class [:runs :dimension] :key (gensym)} "run no"]

     [:th {:class [:songs :metric] :key (gensym)} "total songs"]
     [:th {:class [:motivated :metric] :key (gensym)} "total motivated"]
     (for [id (-> @(re-frame/subscribe [::subs/simulation-runs]) first :observer-counts sort keys)]
       [:th {:class [(str "observer-" id) :metric] :key (gensym)} (str "observer no " id)])]]

   (into [:tbody]
     (for [result @(re-frame/subscribe [::subs/simulation-runs])]
       [:tr {:class :result-row :key (gensym)}
        (for [dimension @(re-frame/subscribe [::subs/simulation-variables])]
          [:td {:class [dimension :dimension] :key (gensym)} (dimension result)])

        [:td {:class [:runs :dimension] :key (gensym)} (:run result)]

        [:td {:class [:songs :metric] :key (gensym)} (:songs result)]
        [:td {:class [:motivated :metric] :key (gensym)} (:motivated result)]
        (for [[id counts] (-> result :observer-counts sort)]
          [:td {:class [(str "observer-" id) :metric] :key (gensym)} counts])]))])

(defn view []
  [:div {:class :simulator :id :simulator}
   [settings]
   [results]])
