(ns birds.views.views
  (:require [re-frame.core :as re-frame]
            [birds.gui :as gui]
            [birds.reports :as reports]
            [birds.views.subs :as subs]
            [birds.views.events :as event]))

(defn render-view []
  [:div
   (gui/controls)
   [:hr]
   (reports/show)
   ])
