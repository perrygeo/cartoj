(ns example.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [cartoj.core :as cartoj]))

(defn app []
  [:div
   [:h2 "Cartoj Hello World"]
   [cartoj/interactive-map
    {:initial-view-state {:longitude 0
                          :latitude 16
                          :zoom 1}
     :map-style "https://tiles.openfreemap.org/styles/bright"}]])

;; End app code, Start main DOM scaffolding

(defonce root (atom nil))

(defn ^:dev/after-load re-render []
  (.render ^js @root (r/as-element [app])))

(defn init []
  (reset! root (rdom/create-root (js/document.getElementById "app")))
  (.render ^js @root (r/as-element [app])))
