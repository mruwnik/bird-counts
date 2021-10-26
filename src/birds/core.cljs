(ns birds.core
  (:require [reagent.dom :as rdom]
            [birds.gui :as gui]
            [birds.reports :as reports]
            [birds.forest :as forest]))

(defn ^:export init []
  (rdom/render [:div
                (gui/controls)
                [:hr]
                (reports/show gui/settings)]
               (js/document.getElementById "controls"))
  (reports/init! gui/settings)
  (gui/initialise!)
  (forest/start-rendering gui/settings))
