(ns birds.views.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [birds.views.subs :as subs]
            [birds.views.html :as html]
            [birds.views.events :as event]
            [birds.reports :as reports]
            [birds.forest :as forest]
            [birds.views.generator :as generator]))

(defn observer-val [id key] @(re-frame/subscribe [::subs/observer-value id key]))

(defn dispatch-global-update [key value notifications]
  (re-frame/dispatch [::event/update-bird-setting (-> key name keyword) value notifications]))

(defn dispatch-item-update [item key value notifications]
  (re-frame/dispatch [::event/update-observer-setting item key value notifications]))

(defn item-inputter [item input-type key desc & notifications]
  (input-type key desc
              (observer-val item key)
              #(dispatch-item-update item key % notifications)))

(defn inputter [input-type key desc propagate-to]
  (input-type key desc
              @(re-frame/subscribe [key])
              #(dispatch-global-update key % propagate-to)))

(defn num-input [key desc & propagate-to] (inputter html/float-input key desc propagate-to))
(defn int-input [key desc & propagate-to] (inputter html/int-input key desc propagate-to))
(defn prob-input [key desc & propagate-to] (inputter html/prob-input key desc propagate-to))
(defn ms-input [key desc & propagate-to] (inputter html/ms-input key desc propagate-to))
(defn checkbox [key desc & propagate-to] (inputter html/checkbox key desc propagate-to))

(defn gui []
  [:div {:class :controls}
   [int-input ::subs/num-of-birds "Number of birds: " ::event/generate-birds]
   [int-input ::subs/volume "Bird volume"]
   [int-input ::subs/audio-sensitivity "how far birds can hear"]
   [prob-input ::subs/spontaneous-sing-prob "Prob of singing in a given tick"]
   [prob-input ::subs/motivated-sing-prob "Prob of motivated singing"]
   [ms-input ::subs/motivated-sing-after "avg time to motivated singing [ticks]"]
   [ms-input ::subs/sing-rest-time "avg time of resting after singing [ticks]"]
   [ms-input ::subs/song-length "avg song length [tick]"]
   [num-input ::subs/speed "speed of simulation" ::event/set-speed]
   [ms-input ::subs/tick-length "tick length [ms]" ::event/set-tick-length]
   [checkbox ::subs/show-birds? "show bird location?"]
   [checkbox ::subs/show-bird-hear? "show bird reaction radius?"]
   [checkbox ::subs/show-observers? "show observer location?"]
   [checkbox ::subs/show-observer-hear? "show observer reaction radius?"]
   [:hr]
   [:details
    [:summary "Bird colours"]
    [inputter html/colour-picker ::subs/bird-colour "Location"]
    [inputter html/colour-picker ::subs/song-colour "Singing radius"]
    [inputter html/colour-picker ::subs/resing-colour "Re-sing radius"]
    [inputter html/colour-picker ::subs/resting-colour "Ignore radius"]
    [inputter html/colour-picker ::subs/hearing-colour "Listening radius"]]])


(defn strategy-selector [observer-id]
  (html/select :strategy "Observer strategy"
               (observer-val observer-id :strategy)
               @(re-frame/subscribe [::subs/observer-strategies])
               #(dispatch-item-update observer-id :strategy (keyword %) nil)))

(defn observer-stats [id]
  [:span {:class :observer-results}
   (str "Heard " (->> (observer-val id :observations) (map :count) (reduce +)) " birds")])

(defn observer-controls [id]
  [:details {:key (gensym) :class :observer :open true}
   [:summary (str "Observer no " id)]
   [item-inputter id html/checkbox :observing "Currently observing?" ::event/toggle-observation]
   [strategy-selector id]
   [item-inputter id html/int-input :audio-sensitivity "How far can the observer hear"]
   [item-inputter id html/colour-picker :observer-colour "The colour of the observer"]
   [item-inputter id html/colour-picker :hearing-colour "The colour of the hearing radius"]
   [observer-stats id]
   [:br]
   [:div {:class :events-downloader}
    "Download all events"
    [:button {:on-click #(re-frame/dispatch [::event/download-observer-data id :csv])} "csv"]
    [:button {:on-click #(re-frame/dispatch [::event/download-observer-data id :json])} "json"]]
   [:button {:on-click #(re-frame/dispatch [::event/remove-observer id])} "Remove observer"]
   [:button {:on-click #(re-frame/dispatch [::event/clear-observer-data id])} "Clear data"]])

(defn observers-block []
  [:div {:class :observer-block}
   [:div {:class :observers}
    (doall (map observer-controls @(re-frame/subscribe [::subs/observer-ids])))]
   [:button {:on-click #(re-frame/dispatch [::event/add-observer])} "Add new observer"]])

(defn render-gui []
  [:div {:id :controls :class [:controls :grid-item]}
   [gui]
   [:hr]
   [observers-block]
   [:hr]
   (reports/show)])

(defn render-forest []
  (r/create-class
   {:component-did-mount
    (fn [component]
      (let [settings (merge {:width (* 0.8 (.-innerWidth js/window))
                             :height (.-innerHeight js/window)}
                            @(re-frame/subscribe [::subs/forest-settings]))]
        (forest/start-rendering (rdom/dom-node component) settings)))
    :render
    (fn [] [:div {:id :forest :class :grid-item}])}))

(defn render-sandbox []
  [:div {:class :sandbox-container}
   [render-forest]
   [render-gui]])

(defn tabbed-windows [tabs]
  (let [selected-tab @(re-frame/subscribe [::subs/selected-tab])]
    [:div {:class :window}
     [:div {:class :tab-headers}
      (for [[id _] tabs]
        [:span {:class (conj (when (= selected-tab id) [:selected]) :tab-header)
                :id id :key (gensym)
                :on-click #(re-frame/dispatch [::event/select-tab id])}
         id])]
     [:div {:class :tabs}
      (for [[id tab] tabs]
        [:div {:class (conj (when (= selected-tab id) [:selected]) :tab)
               :id id :key (gensym)}
         [tab]])]]))

(defn offline []
  [:div
   [:h2 "Java standalone program"]
   [:div
    "To run locally, download "
    [:a {:href "/birds/birds.zip"} "this"] " zip file. Inside is a standalone jar file that can be executed to run the program locally. The obvious benefits of this is that it will be run naively on multiple threads. There is a config file in the zip archive with instructions on how parameters can be changed."]
   [:div "The program is executed by running:" [:br] [:code "java -jar birds-standalone.jar -c config.edn output.csv"]]
   [:div "This will result in an " [:code "output.csv"] " file containing the simulation results. This file is updated in real time, so the simulation can be killed at any time without data loss.J"]

   [:h2 "R package"]
   [:div "The simulation can also be run as an R package, which can be found " [:a {:href "/birds/birdsSimulator_0.1.0.tar.gz"} "here"] "."]
   [:div "Once the package has been installed, use " [:code "vignette(\"introduction\", package='birdsSimulator')"] " to view the documentation for details on how to run simulations."]
   [:div "Or just run " [:code "library('birdsSimulator'); simulate(list(\"num-of-birds\" = c(10)));"]]])

(defn render-view []
  (tabbed-windows
   [[:sandbox render-sandbox]
    [:simulator generator/view]
    [:offline offline]]))
