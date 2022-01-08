(ns birds.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [birds.views.views :as views]
            [birds.views.events :as events]
            [birds.views.events-handler]))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "controls")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/render-view] root-el)))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (mount-root)
  (re-frame/dispatch-sync [::events/initialize-gui])
  (re-frame/dispatch-sync [::events/initialize-reports])
  (re-frame/dispatch-sync [::events/intitialise-observers-watch])
  (re-frame/dispatch-sync [::events/start-bird-loop])
  (re-frame/dispatch-sync [::events/generate-birds])
  (re-frame/dispatch-sync [::events/start-render-forest]))
