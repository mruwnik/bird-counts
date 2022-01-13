(ns birds.views.generator
  (:require [reagent.core :as r]
            [re-frame.core :as re-frame]
            [birds.simulate :as sim]
            [birds.views.subs :as subs]
            [birds.views.events :as events]
            [birds.views.html :as html]
            [birds.bird :as birds]
            [birds.observer :as observers]
            [birds.converters :as conv]))

(defn simulate-for-settings [settings ticks observers]
  (sim/simulate-n-ticks (birds/update-birds-count {} settings) observers ticks))

(when false
  (simulate-for-settings
   @(re-frame/subscribe [::subs/bird-settings])
   1080
   (->> @(re-frame/subscribe [::subs/observers])
        (map #(assoc (observers/new-observer %) :pos (:pos %)))
        (conv/seq->id-map))
   )
  )

(defn select [class desc value options dispatcher]
  [:div {:class [:setting-input class]}
   desc
   [:select {:name class :default-value value :on-change #(-> % .-target .-value dispatcher)}
    (when-not (seq (filter #{value} options))
      [:option {:value nil} "-"])
    (for [option options] [:option {:value option :key (gensym)} option])]])

(def input-types
  {:num-of-birds          html/int-input
   :volume                html/int-input
   :spontaneous-sing-prob html/prob-input
   :motivated-sing-prob   html/prob-input
   :motivated-sing-after  html/ms-input
   :sing-rest-time        html/ms-input
   :song-length           html/ms-input
   :audio-sensitivity     html/int-input})

(defn settings []
  (let [used-options (r/atom {:variables {} :ticks 1000 :times 1})]
    (fn []
      (let [global-options (->> @(re-frame/subscribe [::subs/bird-settings])
                                (remove (comp #{:width :height} first))
                                (into {}))
            available-options (->> global-options keys (remove (:variables @used-options)))]
        [:div {:class :settings}
         [:button {:class :run-simulations :on-click #(re-frame/dispatch [::events/start-simulations @used-options])}
          "Run simulations"]
         [:button {:class :run-simulations :on-click #(re-frame/dispatch [::events/download-simulation-runs :json])}
          "JSON"]
         (html/int-input :ticks "how many ticks per simulation run" (:ticks @used-options) #(swap! used-options assoc :ticks %))
         (html/int-input :times "how many times to run simulation per variable set" (:times @used-options) #(swap! used-options assoc :times %))
         (html/select :simulation-options "Variables" nil available-options
                      #(swap! used-options assoc-in [:variables (keyword %)] [(-> % keyword global-options) (-> % keyword global-options) 0]))
         (for [[key [from to step]] (:variables @used-options)]
           [:div {:class :generator-option :key (gensym)}
            [:label key]
            ((input-types key) key "from" from #(swap! used-options assoc-in [:variables key 0] %))
            ((input-types key) key "to" to #(swap! used-options assoc-in [:variables key 1] %))
            (html/pos-int-input key "step" step #(swap! used-options assoc-in [:variables key 2] %))
            [:button {:class [:remove-option key] :on-click #(swap! used-options update :variables dissoc key)} "X"]
            ])]))))

(defn view []
  [:div {:class :simulator}
   [settings]
   [:div {:class :results}
    [:div {:class :result-header :key (gensym)}
     (for [dimension [:num-of-birds :motivated-sing-prob :spontaneous-sing-prob :motivated-sing-after :sing-rest-time :song-length :volume :audio-sensitivity]]
       [:span {:class [dimension :dimension] :key (gensym)} (name dimension)])

     [:span {:class [:songs :dimension] :key (gensym)} "run no"]

     [:span {:class [:songs :metric] :key (gensym)} "total songs"]
     [:span {:class [:motivated :metric] :key (gensym)} "total motivated"]
     (for [id (-> @(re-frame/subscribe [::subs/simulation-runs]) first :observer-counts sort keys)]
       [:span {:class [(str "observer-" id) :metric] :key (gensym)} (str "observer no " id)])]

    (for [result @(re-frame/subscribe [::subs/simulation-runs])]
     [:div {:class :result-row :key (gensym)}
       (for [dimension [:num-of-birds :motivated-sing-prob :spontaneous-sing-prob :motivated-sing-after :sing-rest-time :song-length :volume :audio-sensitivity]]
         [:span {:class [dimension :dimension] :key (gensym)} (-> result :settings dimension)])

       [:span {:class [:songs :dimension] :key (gensym)} (:run result)]

       [:span {:class [:songs :metric] :key (gensym)} (:songs result)]
       [:span {:class [:motivated :metric] :key (gensym)} (:motivated result)]
       (for [[id counts] (-> result :observer-counts sort)]
         [:span {:class [(str "observer-" id) :metric] :key (gensym)} counts])])]])
