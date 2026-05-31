(ns cartoj.demo
  "Interactive demo / development harness for cartoj."
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom-client]
            [re-frame.core :as rf]
            [cartoj.core :as cartoj]
            [cartoj.overlays :as overlay]
            [cartoj.sources :as sources]
            [cartoj.controls :as ctrl]
            [cartoj.interop :as interop]
            [cartoj.re-frame :as cartoj-rf])
  (:require-macros [cartoj.demo-macros :refer [code-string]]))

(def demo-stylesheet
  "https://pmtiles.perrygeo.com/styles/light.json"
  #_"https://demotiles.maplibre.org/style.json")

(def sf-coords
  {:longitude -122.4194 :latitude 37.7749 :zoom 7})

(defn code-block
  [src]
  [:pre [:code {:class "language-clojure"} src]])

(defn click-event-section []
  (let [click-handler (fn [el]
                        (let [pos (.-lngLat el)]
                          (js/alert
                           (str "clicked lng:" (.toFixed (.-lng pos) 4)
                                " lat:" (.toFixed (.-lat pos) 4)))))]
    (fn []
      [:section
       [:h2 "On click event"]
       [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 10})
                                :map-style demo-stylesheet
                                :class-name "imap"
                                :on-click click-handler}
        [ctrl/navigation-control {:position "top-right"}]]
       ;; TODO
       ;; more information about click like
       ;; https://github.com/visgl/react-map-gl/blob/f295bd524e01b7fc0fb9c9e9d1d5bd47b055b67d/examples/maplibre/custom-cursor/src/app.tsx#L25
       ;; and put that info in a div overlaying the map, instead of an alert
       [:p "Uncontrolled in that clojurescript has no visibility into map state,
          beyond setting the initial view state and responding via registered handlers.
          in this case :on-click"]])))

(defn controls-section []
  [:section
   [:h2 "Interactive map controls"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 1})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]
    [ctrl/fullscreen-control {:position "top-left"}]
    [ctrl/geolocate-control {:position "top-right"}]
    [ctrl/scale-control {:position "bottom-left" :max-width 120 :unit "metric"}]]
   [:p "Map controls"]])

(defn flyto-section []
  (let [map-ref (atom nil)
        selected-city (atom nil)
        cities [{:name "San Francisco" :center [-122.4194 37.7749]}
                {:name "New York" :center [-74.0060 40.7128]}
                {:name "London" :center [-0.1276 51.5074]}
                {:name "Dubai" :center [55.2708 25.2048]}]
        on-select-city (fn [city]
                         (reset! selected-city (:name city))
                         (when-let [^js m @map-ref]
                           (.flyTo m (clj->js {:center (:center city)
                                               :zoom 6
                                               :duration 3000}))))]
    (fn []
      [:section
       [:h2 "Fly To Location"]
       [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 1})
                                :map-style demo-stylesheet
                                :class-name "imap"}
        [interop/with-map (fn [m] (reset! map-ref m) [:<>])]
        [ctrl/navigation-control {:position "top-right"}]]

       [:p "Smooth camera transitions between locations:"
        (for [city cities]
          ^{:key (:name city)}
          [:div
           [:a {:href "#"
                :on-click (fn [e]
                            (.preventDefault e)
                            (on-select-city city))}
            (:name city)]])]])))

(defn geocoder-section []
  [:section
   [:h2 "Geocoding"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 1})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]]
   [:p "Map controls"]])

(defn controlled-section []
  (let [state (or @(rf/subscribe [::cartoj-rf/view-state]) sf-coords)]
    [:section
     [:h2 "Controlled map"]
     [cartoj/interactive-map
      (merge state
             {:map-style demo-stylesheet
              :class-name "imap"
              :on-move (cartoj-rf/on-move)})
      [ctrl/navigation-control {:position "top-right"}]]
     [:table
      [:tbody
       [:tr
        [:th "Longitude"]
        [:th "Latitude"]
        [:th "Zoom"]]
       [:tr
        [:td (.toFixed (:longitude state) 4)]
        [:td (.toFixed (:latitude state) 4)]
        [:td (.toFixed (:zoom state) 1)]]]]
     [:p "By registering an on-move handler, and subscribing to view-state,
          Clojurescript has full access to the map state."]]))

(defn overlay-section []
  (let [show-popup? (r/atom false)]
    (fn []
      [:section
       [:h2  "Marker + Popup"]
       [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 7})
                                :class-name "imap"
                                :map-style demo-stylesheet}
        [overlay/marker
         {:longitude (:longitude sf-coords)
          :latitude  (:latitude sf-coords)
          :on-click  #(swap! show-popup? not)}]
        (when @show-popup?
          [overlay/popup
           {:longitude  (:longitude sf-coords)
            :latitude   (:latitude sf-coords)
            :on-close   #(reset! show-popup? false)
            :close-button true}
           [:h2.popup "San Francisco"]])]])))

(defn point-features-section []
  (let [sample-geojson {:type "FeatureCollection"
                        :features [{:type "Feature"
                                    :geometry {:type "Point" :coordinates [-122.4194 37.7749]}
                                    :properties {:name "San Francisco"}}
                                   {:type "Feature"
                                    :geometry {:type "Point" :coordinates [-118.2437 34.0522]}
                                    :properties {:name "Los Angeles"}}]}]
    (fn []
      [:section
       [:h2  "Point Features"]
       [cartoj/interactive-map {:initial-view-state {:longitude -98 :latitude 38 :zoom 3}
                                :map-style demo-stylesheet
                                :class-name "imap"}
        [sources/source {:id "cities"
                         :type "geojson"
                         :data (clj->js sample-geojson)}
         [sources/layer {:id "cities-circles"
                         :type "circle"
                         :source "cities"
                         :paint {:circle-radius 8
                                 :circle-color "#e00"
                                 :circle-stroke-width 2
                                 :circle-stroke-color "#fff"}}]]]

       [:p "Add point locations programatically.
            The structure of the \"Feature\" and \"FeatureCollection\" is based on the GeoJSON standard."]])))

(defn layer-switcher-section []
  [:section
   [:h2 "⚠️ Layer Switcher"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 3})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]]
   [:p "Choose which layers to display on the map."]])

(defn label-section []
  [:section
   [:h2 "Labeling a GeoJSON HTTP Source"]
   [cartoj/interactive-map {:initial-view-state {:longitude 0 :latitude 0 :zoom 0}
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [sources/source {:id "cities"
                     :type "geojson"
                     :data "/data/ne_110m_populated_places_simple.geojson"}
     [sources/layer {:id "cities-circles"
                     :type "circle"
                     :source "cities"
                     :paint {:circle-radius 3
                             :circle-color "#fff"
                             :circle-stroke-width 1
                             :circle-stroke-color "#a99"}}]
     [sources/layer {:id "cities-labels"
                     :type "symbol"
                     :source "cities"
                     :layout {:text-field ["get" "name"]
                              :text-size 11
                              :text-offset [0 0]
                              :text-anchor "top"}
                     :paint {:text-color "#333"
                             :text-halo-color "rgba(255,255,255,0.85)"
                             :text-halo-width 2
                             :text-halo-blur 1}}]]]
   [:p "Add a layer to label a source by attribute."]])

(defn geojson-http-section []
  (let [sample-geojson "/data/ne_110m_populated_places_simple.geojson"]
    (fn []
      [:section
       [:h2 "GeoJSON HTTP Source"]
       [cartoj/interactive-map {:initial-view-state {:longitude 0 :latitude 0 :zoom 0}
                                :map-style demo-stylesheet
                                :class-name "imap"}
        [sources/source {:id "cities"
                         :type "geojson"
                         :data (clj->js sample-geojson)}
         [sources/layer {:id "cities-circles"
                         :type "circle"
                         :source "cities"
                         :paint {:circle-radius 3
                                 :circle-color "#fff"
                                 :circle-stroke-width 1
                                 :circle-stroke-color "#a99"}}]]]

       [:p "Loads features from a GeoJSON source using an unauthenticated url."]
       [:p "Maplibre will perform the HTTP request, error handling, and parsing."]])))

(defn heatmap-section []
  (let [points "/data/earthquakes.geojson"]
    (fn []
      [:section
       [:h2 "Heatmap"]
       [cartoj/interactive-map {:initial-view-state {:longitude -179.9 :latitude 30 :zoom 0.8}
                                :map-style demo-stylesheet
                                :class-name "imap"}
        [sources/source {:id "earthquakes" :type "geojson" :data points}
         [sources/layer {:id "earthquake-heatmap"
                         :type "heatmap"
                         :source "earthquakes"
                         :paint {:heatmap-intensity 0.7
                                 :heatmap-color ["interpolate" ["linear"] ["heatmap-density"]
                                                 0 "rgba(33,102,172,0)"
                                                 0.2 "rgb(103,169,207)"
                                                 0.4 "rgb(209,229,240)"
                                                 0.6 "rgb(253,219,199)"
                                                 0.9 "rgb(239,138,98)"
                                                 1 "rgb(178,24,43)"]
                                 :heatmap-radius 17
                                 :heatmap-opacity 0.7}}]]]
       [:p "Density heatmap of earthquakes rendered from a GeoJSON HTTP resource."]
       [:p "TODO source"]])))

(defn cluster-section []
  (let [earthquake-data "/data/earthquakes.geojson"
        map-ref (atom nil)
        on-click (fn [^js e]
                   (when-let [^js m @map-ref]
                     (let [^js features (.-features e)
                           ^js feature  (when features (aget features 0))]
                       (when feature
                         (let [^js props   (.-properties feature)
                               cluster-id (.-cluster_id props)]
                           (when cluster-id
                             (let [^js source (.getSource m "earthquakes")]
                               (-> (.getClusterExpansionZoom source cluster-id)
                                   (.then (fn [zoom]
                                            (.easeTo m (clj->js
                                                        {:center   (.-coordinates (.-geometry feature))
                                                         :zoom     zoom
                                                         :duration 500}))))))))))))
        cluster-layer {:id "clusters"
                       :type "circle"
                       :source "earthquakes"
                       :filter ["has" "point_count"]
                       :paint {:circle-color ["step" ["get" "point_count"] "#51bbd6" 100 "#f1f075" 750 "#f28cb1"]
                               :circle-radius ["step" ["get" "point_count"] 20 100 30 750 40]}}
        cluster-count-layer {:id "cluster-count"
                             :type "symbol"
                             :source "earthquakes"
                             :filter ["has" "point_count"]
                             :layout {:text-field "{point_count_abbreviated}"
                                      :text-size 12}}
        unclustered-point-layer {:id "unclustered-point"
                                 :type "circle"
                                 :source "earthquakes"
                                 :filter ["!" ["has" "point_count"]]
                                 :paint {:circle-color "#11b4da"
                                         :circle-radius 4
                                         :circle-stroke-width 1
                                         :circle-stroke-color "#fff"}}]
    (fn []
      [:section
       [:h2 "Cluster"]
       [cartoj/interactive-map {:initial-view-state {:longitude -122.4194 :latitude 37.7749 :zoom 1}
                                :map-style demo-stylesheet
                                :class-name "imap"
                                :interactive-layer-ids ["clusters"]
                                :on-click on-click}
        [interop/with-map
         (fn [m] (reset! map-ref m) [:<>])]
        [sources/source {:id "earthquakes"
                         :type "geojson"
                         :data earthquake-data
                         :cluster true
                         :cluster-max-zoom 14
                         :cluster-radius 50}
         [sources/layer cluster-layer]
         [sources/layer cluster-count-layer]
         [sources/layer unclustered-point-layer]]]
       [:p "Group nearby points into clusters at low zoom levels. Click a cluster to expand it."]])))

(defn limit-interactivity-section []
  (let [sfbay-bounds [[-123.4 37.2] [-121.3 38.5]]]
    (fn []
      [:section
       [:h2 "Limit Interactivity"]
       [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 8})
                                :map-style demo-stylesheet
                                :class-name "imap"
                                :max-bounds sfbay-bounds
                                :min-zoom 7
                                :max-zoom 12
                                :drag-rotate false}
        [ctrl/navigation-control {:position "top-right"}]]
       [:p "Panning restricted to the San Francisco Bay Area. Zoom limited to 8-14. Rotation disabled."]])))

(defn dynamic-styling-section []
  [:section
   [:h2 "⚠️ Dynamic Styling"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 10})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]]
   [:p "Change map or layer styles programmatically in response to user input."]])

(defn style-by-numeric-section []
  [:section
   [:h2 "⚠️ Style by numeric properties"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 3})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]]
   [:p "Choropleth"]])

(defn style-by-category-section []
  [:section
   [:h2 "⚠️ Style by categorical property"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 3})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]]
   [:p "Style according to unique values"]])

(defn side-by-side-section []
  [:section
   [:h2 "⚠️ Side by Side"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 3})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]]
   [:p "Compare two map styles or layers side by side."]])

(defn terrain-section []
  [:section
   [:h2 "⚠️ Terrain"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 5})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]]
   [:p "Enable 3D terrain rendering on the map."]])

(defn drawing-section []
  [:section
   [:h2 "⚠️ Drawing Features"]
   [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 10})
                            :map-style demo-stylesheet
                            :class-name "imap"}
    [ctrl/navigation-control {:position "top-right"}]]
   [:p "Draw and edit geometries directly on the map."]])

(defn barebones-section []
  [:section
   [cartoj/interactive-map {:map-style  "https://demotiles.maplibre.org/style.json"
                            :class-name "imap"}]
   [:p "Minimum viable interactive map."]])

;; In general we want to use Reagent's Form-2 component pattern. 
;;   The outer function runs once on mount, returning a closure. 
;;   The inner function is called by Reagent on each re-render,
;;   so the reagant atom holding state persists across renders.

(defn basemaps-section []
  (let [basemap-styles [{:name "[OpenFreeMap] Liberty (Street)" :url "https://tiles.openfreemap.org/styles/liberty"}
                        {:name "[OpenFreeMap] Bright"           :url "https://tiles.openfreemap.org/styles/bright"}
                        {:name "[OpenFreeMap] Positron (Light)" :url "https://tiles.openfreemap.org/styles/positron"}
                        {:name "[OpenFreeMap] Dark Matter"      :url "https://tiles.openfreemap.org/styles/dark"}
                        {:name "[Protomaps] Light"              :url "https://pmtiles.perrygeo.com/styles/light.json"}
                        {:name "[Protomaps] Dark"               :url "https://pmtiles.perrygeo.com/styles/dark.json"}
                        {:name "[Protomaps] Data Viz White"     :url "https://pmtiles.perrygeo.com/styles/white.json"}
                        {:name "[Protomaps] Data Viz Grey"      :url "https://pmtiles.perrygeo.com/styles/grey.json"}
                        {:name "[Protomaps] Data Viz Black"     :url "https://pmtiles.perrygeo.com/styles/black.json"}]
        selected-style (r/atom (:url (first basemap-styles)))]
    (fn []
      [:section
       [:h2 "Basemaps"]
       [cartoj/interactive-map {:initial-view-state {:latitude 20 :zoom 0.7}
                                :map-style @selected-style
                                :class-name "imap"}
        [ctrl/navigation-control {:position "top-right"}]]
       [:p "Demonstrate switching between different public basemaps"]
       [:div.control-panel
        (for [{:keys [name url]} basemap-styles]
          ^{:key name}
          [:a {:href "#"
               :on-click (fn [e]
                           (.preventDefault e)
                           (reset! selected-style url))
               :style (when (= @selected-style url) {:font-weight "bold"})}
           name])]])))

(defonce selected-tab (r/atom :barebones))

(def tabs
  (sorted-map
   ;; TODO 
   ;; draggable marker
   ;; filter features
   ;; style by client side filtering (typeahead)
   ;; custom MVT source
   ;; Time series
   :barebones         {:title "Barebones"
                       :section barebones-section}
   :basemaps         {:title "Basemaps"
                      :section basemaps-section}
   :click-event      {:title "Click event handler"
                      :section click-event-section}
   :cluster          {:title "Cluster"
                      :section cluster-section}
   :controls         {:title "Interactive Map Controls"
                      :section controls-section}
   :drawing          {:title "⚠️ Drawing Features"
                      :section drawing-section}
   :dynamic-style    {:title "⚠️ Dynamic Styling"
                      :section dynamic-styling-section}
   :flyto            {:title "Fly To Location"
                      :section flyto-section}
   :geocoder         {:title "⚠️ Geocoder"
                      :section geocoder-section}
   :geojson-http     {:title "GeoJSON HTTP (maplibre)"
                      :section geojson-http-section}
   :geojson-manual   {:title "⚠️ GeoJSON HTTP (manual)"
                      :section geojson-http-section}
   :heatmap          {:title "Heatmap"
                      :section heatmap-section}
   :labels           {:title "Labeling"
                      :section label-section}
   :layer-switcher   {:title "⚠️ Layer Switcher"
                      :section layer-switcher-section}
   :limit-interact   {:title "Limit Interactivity"
                      :section limit-interactivity-section}
   :move-event       {:title "⚠️ Mouse move event handler"
                      :section click-event-section}
   :overlays         {:title "Marker + Popup"
                      :section overlay-section}
   :reframe          {:title "Reframe on-move event"
                      :section controlled-section}
   :side-by-side     {:title "⚠️ Side by Side"
                      :section side-by-side-section}
   :sources          {:title "Point Features"
                      :section point-features-section}
   :style-by-category {:title "⚠️ Style by categorical"
                       :section style-by-category-section}
   :style-by-numeric {:title "⚠️ Style by numeric"
                      :section style-by-numeric-section}
   :terrain          {:title "⚠️ Terrain"
                      :section terrain-section}))
(defn sidebar []
  (into [:div.demo-sidebar]
        (for [[id {:keys [title]}] tabs]
          [:a
           {:key      (name id)
            :class    (str "nav-item" (when (= @selected-tab id) " active"))
            :on-click #(reset! selected-tab id)}
           title])))

(defn main-panel []
  (let [{:keys [section]} (get tabs @selected-tab)]
    [:div.demo-main
     [:div [section]]
     [:div (case @selected-tab
             :click-event (code-block (code-string click-event-section))
             :controls    (code-block (code-string controls-section))
             :reframe     (code-block (code-string controlled-section))
             :overlays    (code-block (code-string overlay-section))
             :geocoder    (code-block (code-string geocoder-section))
             :flyto       (code-block (code-string flyto-section))
             :sources          (code-block (code-string point-features-section))
             :layer-switcher   (code-block (code-string layer-switcher-section))
             :geojson-http     (code-block (code-string geojson-http-section))
             :heatmap          (code-block (code-string heatmap-section))
             :cluster          (code-block (code-string cluster-section))
             :limit-interact   (code-block (code-string limit-interactivity-section))
             :dynamic-style    (code-block (code-string dynamic-styling-section))
             :side-by-side     (code-block (code-string side-by-side-section))
             :terrain          (code-block (code-string terrain-section))
             :drawing          (code-block (code-string drawing-section))
             :labels         (code-block (code-string label-section))
             :basemaps       (code-block (code-string basemaps-section))
             :barebones       (code-block (code-string barebones-section))
             [:code "placeholder"])]]))

(defn app []
  [:div#app
   [:header.demo-header
    [:h1 "Cartoj Demo"]]
   [:div.demo-layout
    [sidebar]
    [main-panel]]])

(defonce root (atom nil))

(defn ^:dev/after-load re-render []
  (.render @root (r/as-element [app])))

(defn init []
  (rf/dispatch-sync [::cartoj-rf/set-view-state sf-coords])
  (reset! root (rdom-client/create-root (js/document.getElementById "app")))
  (.render @root (r/as-element [app])))

(comment
  (re-render)
  (init)
  (namespace ::x))
