(ns birds.views.views
  (:require [birds.gui :as gui]
            [birds.reports :as reports]
            [birds.observer :as observer]))

(defn render-view []
  [:div
   (gui/controls)
   [:hr]
   (observer/controls)
   [:hr]
   (reports/show)])
