(ns birds.core
  (:require [reagent.dom :as rdom]
            [birds.gui :as gui]
            [birds.forest :as forest]))

(defn ^:export init []
  (rdom/render [gui/controls] (js/document.getElementById "controls"))
  (gui/initialise!)
  (forest/start-rendering gui/settings))
