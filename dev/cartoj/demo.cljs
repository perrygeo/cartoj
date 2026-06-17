(ns cartoj.demo
  "Interactive demo / development harness for cartoj."
  (:require [cartoj.controls :as ctrl]
            [cartoj.core :as cartoj]
            [cartoj.draw :as draw]
            [cartoj.geocoder :as geocoder]
            [cartoj.interop :as interop]
            [cartoj.overlays :as overlay]
            [cartoj.re-frame :as cartoj-rf]
            [cartoj.sources :as sources]
            [cljs.pprint :refer [pprint]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom.client :as rdom-client])
  (:require-macros [cartoj.demo-macros :refer [code-string]]))

;; ## Utilities

(defn code-block-render
  "Pure render function: returns hiccup for a syntax-highlighted code block.
   Testable without a DOM."
  [src]
  [:pre [:code {:class "language-clojure"} src]])

(defn code-block
  "Reagent Form-3 component. A :ref alone is insufficient because React
   only fires it on mount/unmount; tab switches reuse the same <code>
   element and just swap the text node, wiping the hljs spans. So we
   re-highlight from :component-did-mount AND :component-did-update."
  [_src]
  (let [el-ref     (atom nil)
        save-ref!  (fn [el] (reset! el-ref el))
        highlight! (fn [_this]
                     (when-let [el @el-ref]
                       (js-delete (.-dataset el) "highlighted")
                       (js/hljs.highlightElement el)))]
    (r/create-class
     {:display-name                                     "code-block"
      :component-did-mount                              highlight!
      :component-did-update                             highlight!
      :reagent-render
      (fn [src]
        [:pre.clojure [:code {:class "language-clojure"
                              :ref   save-ref!} src]])})))

(defn table-repr
  "Print a reasonable HTML table representation of a clojure map"
  [m]
  [:table
   [:tbody
    (for [[k v] m]
      [:tr
       [:th (str k)]
       [:td (if (coll? v)
              (with-out-str (pprint v))
              (str v))]])]])

(defn atom-watcher
  "Used with `add-watch` to inspect an atom over time and print any changes.
    (add-watch app-state :app-state-watcher atom-watcher)"
  [key _the-atom old-state new-state]
  (println "Atom" key "changed"
           "\nOld state:" old-state
           "\nNew state:" new-state))

;; ## App State

(def default-stylesheet "https://tiles.openfreemap.org/styles/positron")

(def sf-coords {:longitude -122.4194 :latitude 37.7749 :zoom 7})

(defonce selected-tab (r/atom :barebones))

(comment  ;; inspect app state from the REPL!
  (@selected-tab)
  (reset! selected-tab :drawing)
  (add-watch selected-tab :map-ref-watcher atom-watcher)
  (remove-watch selected-tab :map-ref-watcher))

;; ## Example sections
;;
;; In general these examples use the Reagent Form-2 component pattern
;;  to keep the relevant state contained in one component source.
;;  The outer function runs once on mount, returning a closure.
;;  The inner function is called by Reagent on each re-render,
;;  so the reagant atom holding state persists across renders.
;; This is good for examples but real apps may want to hold state in other ways.

(defn barebones-section []
  [:section
   [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                            :map-style          "https://demotiles.maplibre.org/style.json"}]
   [:p "Minimum viable interactive map."]])

(defn basemaps-section []
  (let [basemap-styles [{:name "[OpenFreeMap] Liberty (Street)" :url "https://tiles.openfreemap.org/styles/liberty"}
                        {:name "[OpenFreeMap] Bright" :url "https://tiles.openfreemap.org/styles/bright"}
                        {:name "[OpenFreeMap] Positron (Light)" :url "https://tiles.openfreemap.org/styles/positron"}
                        {:name "[OpenFreeMap] Dark Matter" :url "https://tiles.openfreemap.org/styles/dark"}
                        {:name "[Protomaps] Light" :url "https://pmtiles.perrygeo.com/styles/light.json"}
                        {:name "[Protomaps] Dark" :url "https://pmtiles.perrygeo.com/styles/dark.json"}
                        {:name "[Protomaps] Data Viz White" :url "https://pmtiles.perrygeo.com/styles/white.json"}
                        {:name "[Protomaps] Data Viz Grey" :url "https://pmtiles.perrygeo.com/styles/grey.json"}
                        {:name "[Protomaps] Data Viz Black" :url "https://pmtiles.perrygeo.com/styles/black.json"}]
        selected-style (r/atom (:url (first basemap-styles)))]
    (fn []
      [:section
       [:h2 "Basemaps"]
       [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                                :map-style          @selected-style}
        [ctrl/navigation-control {:position "top-right"}]]
       [:p "Demonstrate switching between different public basemaps"]
       [:div.control-panel
        (for [{:keys [name url]} basemap-styles]
          ^{:key name}
          [:a {:href     "#"
               :on-click (fn [e]
                           (.preventDefault e)
                           (reset! selected-style url))
               :style    (when (= @selected-style url) {:font-weight "bold"})}
           name])]])))

(defn expand-cluster [map-ref e source-name]
  (when-let [^js m @map-ref]
    (let [^js features (.-features e)
          ^js feature  (when features (aget features 0))]
      (when feature
        (let [^js props  (.-properties feature)
              cluster-id (.-cluster_id props)]
          (when cluster-id
            (let [^js source (.getSource m source-name)]
              (-> (.getClusterExpansionZoom source cluster-id)
                  (.then (fn [zoom]
                           (.easeTo m (clj->js
                                       {:center   (.-coordinates (.-geometry feature))
                                        :zoom     zoom
                                        :duration 500}))))))))))))

(defn cluster-section []
  (let [earthquake-data         "data/earthquakes.geojson"
        map-ref                 (atom nil)
        on-click                (fn [evt] (expand-cluster map-ref evt "earthquakes"))
        cluster-layer           {:id     "clusters"
                                 :type   "circle"
                                 :source "earthquakes"
                                 :filter ["has" "point_count"]
                                 :paint  {:circle-color  ["step" ["get" "point_count"]
                                                          "#51bbd6" 100
                                                          "#f1f075" 750
                                                          "#f28cb1"]
                                          :circle-radius ["step" ["get" "point_count"]
                                                          20 100
                                                          30 750
                                                          40]}}
        cluster-count-layer     {:id     "cluster-count"
                                 :type   "symbol"
                                 :source "earthquakes"
                                 :filter ["has" "point_count"]
                                 :layout {:text-field "{point_count_abbreviated}"
                                          :text-size  12}}
        unclustered-point-layer {:id     "unclustered-point"
                                 :type   "circle"
                                 :source "earthquakes"
                                 :filter ["!" ["has" "point_count"]]
                                 :paint  {:circle-color        "#11b4da"
                                          :circle-radius       4
                                          :circle-stroke-width 1
                                          :circle-stroke-color "#fff"}}]
    (fn []
      [:section
       [:h2 "Cluster"]
       [cartoj/interactive-map {:initial-view-state    {:latitude 16 :zoom 1}
                                :map-style             default-stylesheet
                                :interactive-layer-ids ["clusters"]
                                :on-click              on-click}
        [interop/reset-map-ref! map-ref]
        [sources/source {:id               "earthquakes"
                         :type             "geojson"
                         :data             earthquake-data
                         :cluster          true
                         :cluster-max-zoom 14
                         :cluster-radius   50}
         [sources/layer cluster-layer]
         [sources/layer cluster-count-layer]
         [sources/layer unclustered-point-layer]]]
       [:p "Group nearby points into clusters at low zoom levels. Click a cluster to expand it."]])))

(defn on-mouse-move-event-section []
  (let [last-point       (r/atom nil)
        position-handler (fn [^js e]
                           (reset! last-point (interop/coords-from-evt e)))]
    (fn []
      [:section
       [:h2 "On mouse move event"]
       [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                                :map-style          default-stylesheet
                                :on-mouse-move      position-handler}
        [ctrl/navigation-control {:position "top-right"}]]
       [:table {:style {:width "250px"}}
        [:tbody
         [:tr
          [:th "Longitude"]
          [:td (if-let [lng (:longitude @last-point)]
                 (.toFixed lng 4) "-")]]
         [:tr
          [:th "Latitude"]
          [:td (if-let [lat (:latitude @last-point)]
                 (.toFixed lat 4) "-")]]]]
       [:p "Get the coordinates of your cursor as the mouse moves."]])))

(defn on-click-event-section []
  (let [last-point    (r/atom nil)
        click-handler (fn [^js e]
                        (reset! last-point (interop/coords-from-evt e)))]
    (fn []
      [:section
       [:h2 "On click event"]
       [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                                :map-style          default-stylesheet
                                :on-click           click-handler}
        [ctrl/navigation-control {:position "top-right"}]]
       [:table {:style {:width "250px"}}
        [:tbody
         [:tr
          [:th "Longitude"]
          [:td (if-let [lng (:longitude @last-point)]
                 (.toFixed lng 4) "-")]]
         [:tr
          [:th "Latitude"]
          [:td (if-let [lat (:latitude @last-point)]
                 (.toFixed lat 4) "-")]]]]
       [:p "Get the coordinates of a map click."]])))

(defn controls-section []
  [:section
   [:h2 "Interactive map controls"]
   [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                            :map-style          default-stylesheet}
    [ctrl/navigation-control {:position "top-right"}]
    [ctrl/fullscreen-control {:position "top-left"}]
    [ctrl/geolocate-control {:position "top-right"}]
    [ctrl/scale-control {:position "bottom-left" :max-width 120 :unit "metric"}]]
   [:p "Map controls"]])

(defn flyto-section []
  (let [map-ref        (atom nil)
        selected-city  (atom nil)
        cities         [{:name "San Francisco" :center [-122.4194 37.7749]}
                        {:name "New York" :center [-74.0060 40.7128]}
                        {:name "London" :center [-0.1276 51.5074]}
                        {:name "Dubai" :center [55.2708 25.2048]}]
        on-select-city (fn [city]
                         (reset! selected-city (:name city))
                         (when-let [^js m @map-ref]
                           (.flyTo m (clj->js {:center   (:center city)
                                               :zoom     6
                                               :duration 3000}))))]
    (fn []
      [:section
       [:h2 "Fly To Location"]
       [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                                :map-style          default-stylesheet}
        [interop/reset-map-ref! map-ref]
        [ctrl/navigation-control {:position "top-right"}]]

       [:div "Smooth camera transitions between locations:"
        [:ul
         (for [city cities]
           ^{:key (:name city)}
           [:li
            [:a {:href     "#"
                 :on-click (fn [e]
                             (.preventDefault e)
                             (on-select-city city))}
             (:name city)]])]]])))

(defn geocoder-section []
  [:section
   [:h2 "Geocoder"]
   [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                            :map-style          default-stylesheet}
    [geocoder/geocoder-control {:position  "bottom-left"
                                :marker    true
                                :on-result (fn [^js evt]
                                             (js/console.log "geocoder result:" (.-result evt)))}]]
   [:p "Search for places using Nominatim geocoding."]])

(defn on-move-event-section []
  (let [view-state      (r/atom {:longitude -121.99,
                                 :latitude  37.52,
                                 :zoom      7,
                                 :bearing   41,
                                 :pitch     28})
        on-move-handler (fn [^js evt]
                          (reset! view-state (cartoj/view-state->clj (.-viewState evt))))]
    (fn []
      (let [state @view-state]
        [:section
         [:h2 "On Map Move (changing viewport)"]
         [cartoj/interactive-map
          (merge state
                 {:map-style default-stylesheet
                  :on-move   on-move-handler})
          [ctrl/navigation-control {:position "top-right"}]]
         [table-repr state]
         [:p "By registering an on-move handler, and subscribing to view-state,
              Clojurescript has full access to the map state."]]))))

(defn reframe-section []
  (let [state (or @(rf/subscribe [::cartoj-rf/view-state]) sf-coords)]
    [:section
     [:h2 "Reframe-controlled map"]
     [cartoj/interactive-map
      (merge state
             {:map-style default-stylesheet
              :on-move   (cartoj-rf/on-move)})
      [ctrl/navigation-control {:position "top-right"}]]
     [table-repr state]
     [:p "By registering an on-move handler, and subscribing to view-state,
          Clojurescript has full access to the map state."]]))

(defn overlay-section []
  (let [show-popup? (r/atom false)]
    (fn []
      [:section
       [:h2  "Marker + Popup"]
       [cartoj/interactive-map {:map-style default-stylesheet}
        [overlay/marker
         {:longitude (:longitude sf-coords)
          :latitude  (:latitude sf-coords)
          :on-click  #(swap! show-popup? not)}]
        (when @show-popup?
          [overlay/popup
           {:longitude    (:longitude sf-coords)
            :latitude     (:latitude sf-coords)
            :on-close     #(reset! show-popup? false)
            :close-button true}
           [:div.popup
            [:h2 "San Francisco"]
            [:img {:src   "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0c/GoldenGateBridge-001.jpg/1280px-GoldenGateBridge-001.jpg"
                   :alt   "Golden Gate Bridge, San Francisco"
                   :style {:width "100%" :border-radius "4px"}}]]])]])))

(defn point-features-section []
  (let [sample-geojson {:type     "FeatureCollection"
                        :features [{:type       "Feature"
                                    :geometry   {:type "Point" :coordinates [-122.4194 37.7749]}
                                    :properties {:name "San Francisco"}}
                                   {:type       "Feature"
                                    :geometry   {:type "Point" :coordinates [-118.2437 34.0522]}
                                    :properties {:name "Los Angeles"}}]}]
    (fn []
      [:section
       [:h2  "GeoJSON Features"]
       [cartoj/interactive-map {:initial-view-state {:longitude -98 :latitude 38 :zoom 3}
                                :map-style          default-stylesheet}
        [sources/source {:id   "cities"
                         :type "geojson"
                         :data (clj->js sample-geojson)}
         [sources/layer {:id     "cities-circles"
                         :type   "circle"
                         :source "cities"
                         :paint  {:circle-radius       8
                                  :circle-color        "#e00"
                                  :circle-stroke-width 2
                                  :circle-stroke-color "#fff"}}]]]

       [:p "Add point locations programatically.
            The structure of the \"Feature\" and \"FeatureCollection\" is based on the GeoJSON standard."]])))

(defn layer-interactivity-section []
  (let [;; Layer-id groups based on OpenFreeMap's Positron stylesheet.
        groups        {:roads     ["highway_path" "highway_minor"
                                   "highway_major_casing" "highway_major_inner"
                                   "highway_major_subtle"
                                   "highway_motorway_casing" "highway_motorway_inner"
                                   "highway_motorway_subtle"
                                   "tunnel_motorway_casing" "tunnel_motorway_inner"
                                   "highway_motorway_bridge_casing"
                                   "highway_motorway_bridge_inner"
                                   "road_area_pier" "road_pier"]
                       :water     ["water" "water_intermittent" "water_pattern"
                                   "waterway"]
                       :buildings ["building" "building-top"]
                       :land      ["park" "landuse_residential" "landcover_wood"
                                   "landcover_glacier" "landcover_ice_shelf"
                                   "landcover_sand" "landcover_farmland"
                                   "landcover_rock" "landcover_wetland"]}
        labels        [[:roads "Roads"]
                       [:water "Water"]
                       [:buildings "Buildings"]
                       [:land "Land"]]
        active        (r/atom #{:roads :water :buildings :land})
        cursor        (r/atom "auto")
        clicked       (r/atom nil)
        on-mouse-move (fn [^js evt]
                        (let [^js features (.-features evt)
                              ^js feature  (when features (aget features 0))]
                          (when feature
                            (reset! clicked
                                    {:layer-id   (.. feature -layer -id)
                                     :properties (js->clj (.-properties feature)
                                                          :keywordize-keys true)}))))
        on-enter      (fn [_] (reset! cursor "pointer"))
        on-leave      (fn [_] (reset! cursor "auto") (reset! clicked nil))
        toggle        (fn [k]
                        (swap! active (fn [s] (if (contains? s k) (disj s k) (conj s k)))))]
    (fn []
      (let [ids             (into [] (mapcat groups) @active)
            ;; ["nonexist"] when empty so mouse-enter/leave never fire on a
            ;; feature, matching the upstream custom-cursor example.
            interactive-ids (if (seq ids) ids ["nonexist"])]
        [:section
         [:h2 "Layer Interactivity"]
         [cartoj/interactive-map
          {:initial-view-state    (merge sf-coords {:zoom 12})
           :map-style             default-stylesheet
           :cursor                @cursor
           :interactive-layer-ids interactive-ids
           :on-mouse-move         on-mouse-move
           :on-mouse-enter        on-enter
           :on-mouse-leave        on-leave}
          [ctrl/navigation-control {:position "top-right"}]
          ;; Invisible landcover layers for hit-testing only.
          (for [[id class] [["landcover_grass" "grass"]
                            ["landcover_sand" "sand"]
                            ["landcover_farmland" "farmland"]
                            ["landcover_rock" "rock"]
                            ["landcover_wetland" "wetland"]]]
            ^{:key id}
            [sources/layer {:id           id
                            :type         "fill"
                            :source       "openmaptiles"
                            :source-layer "landcover"
                            :filter       ["==" ["get" "class"] class]
                            :paint        {:fill-color   "rgba(0,0,0,0)"
                                           :fill-opacity 0}}])]
         [:p "Pick which layer categories should be interactive. The cursor
              swaps to a pointer while hovering an interactive feature;
              clicking shows what was hit."]
         [:div.control-panel
          (for [[k label] labels]
            ^{:key (name k)}
            [:label
             [:input {:type      "checkbox"
                      :checked   (contains? @active k)
                      :on-change (fn [_] (toggle k))}]
             label])]
         (if-let [c @clicked]
           [:pre [:code (with-out-str (pprint c))]]
           [:p {:style {:color "#888"}}
            "No feature selected yet — mouse over a feature from an interactive layer on the map."])]))))

(defn layer-switcher-section []
  (let [map-ref       (atom nil)
        visibility    (r/atom {:road true :places true})
        road-layers   ["highway_path" "highway_minor" "highway_major_casing"
                       "highway_major_inner" "highway_major_subtle"
                       "highway_motorway_casing" "highway_motorway_inner"
                       "highway_motorway_subtle" "tunnel_motorway_casing"
                       "tunnel_motorway_inner" "highway_motorway_bridge_casing"
                       "highway_motorway_bridge_inner" "road_area_pier" "road_pier"
                       "highway-name-path" "highway-name-minor" "highway-name-major"
                       "highway-shield-non-us" "highway-shield-us-interstate"
                       "road_shield_us"]
        places-layers ["label_other" "label_village" "label_town" "label_city"
                       "label_city_capital" "label_state" "label_country_1"
                       "label_country_2" "label_country_3" "airport"
                       "waterway_line_label" "water_name_point_label"
                       "water_name_line_label"]
        toggle-group  (fn [layers visible?]
                        (when-let [^js m @map-ref]
                          (let [val (if visible? "visible" "none")]
                            (doseq [layer layers]
                              (.setLayoutProperty m layer "visibility" val)))))]
    (fn []
      [:section
       [:h2 "Layer Switcher"]
       [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 11})
                                :map-style          default-stylesheet}
        [interop/reset-map-ref! map-ref]
        [ctrl/navigation-control {:position "top-right"}]]
       [:p "Toggle the visibility of layers built into the OpenFreeMap default stylesheet:"]
       [:div.control-panel
        [:label
         [:input {:type      "checkbox"
                  :checked   (:road @visibility)
                  :on-change (fn [_]
                               (swap! visibility update :road not)
                               (toggle-group road-layers (:road @visibility)))}]
         "Roads"]
        [:label
         [:input {:type      "checkbox"
                  :checked   (:places @visibility)
                  :on-change (fn [_]
                               (swap! visibility update :places not)
                               (toggle-group places-layers (:places @visibility)))}]
         "Places"]]])))

(defn label-section []
  [:section
   [:h2 "Labeling a GeoJSON HTTP Source"]
   [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                            :map-style          default-stylesheet}
    [sources/source {:id   "cities"
                     :type "geojson"
                     :data "data/ne_110m_populated_places_simple.geojson"}
     [sources/layer {:id     "cities-circles"
                     :type   "circle"
                     :source "cities"
                     :paint  {:circle-radius       3
                              :circle-color        "#fff"
                              :circle-stroke-width 1
                              :circle-stroke-color "#a99"}}]
     [sources/layer {:id     "cities-labels"
                     :type   "symbol"
                     :source "cities"
                     :layout {:text-field  ["get" "name"]
                              :text-size   11
                              :text-offset [0 0]
                              :text-anchor "top"}
                     :paint  {:text-color      "#333"
                              :text-halo-color "rgba(255,255,235,0.85)"
                              :text-halo-width 2
                              :text-halo-blur  1}}]]]
   [:p "Add a layer to label a source by attribute."]])

(defn geojson-manual-http-section []
  (let [collection      (r/atom (clj->js {}))
        waiting-message (r/atom nil)
        fetch-json-with (fn [url callback]
                          (-> (js/fetch url)
                              (.then (fn [response] (.json response)))
                              (.then (fn [js-data] (callback js-data)))
                              (.catch (fn [error] (.error js/console "Fetch failed:" error)))))
        reset-handler   (fn [_]
                          (reset! collection  (clj->js {})))
        click-handler   (fn [_]
                          (reset! collection  (clj->js {}))
                          (reset! waiting-message "fetching cities geojson...")
                          (fetch-json-with "data/ne_110m_populated_places_simple.geojson" #(reset! collection  %))
                          (js/setTimeout #(reset! waiting-message nil) 960))]
    (fn []
      [:section
       [:h2 "GeoJSON, manual HTTP"]
       [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                                :map-style          default-stylesheet}
        [sources/source {:id   "cities"
                         :type "geojson"
                         :data @collection}
         [sources/layer {:id     "cities-circles"
                         :type   "circle"
                         :source "cities"
                         :paint  {:circle-radius       3
                                  :circle-color        "#ffb"
                                  :circle-stroke-width 1
                                  :circle-stroke-color "#a99"}}]]]
       [:button {:on-click click-handler} "Load cities"
        (when @waiting-message [:span {:class "waiting"} (str @waiting-message)])]
       [:button {:on-click reset-handler} "Unload"]
       [:pre [:code (str (js->clj @collection :keywordize-keys true ()))]]
       [:p "Sometimes you need more control over the HTTP request"]
       [:ul
        [:li "data fetched by custom event handlers"]
        [:li "addtional HTTP headers or authentication handshakes"]
        [:li "error handling"]
        [:li "parsing, filtering, transforming..."]]])))

(defn geojson-http-section []
  [:section
   [:h2 "GeoJSON HTTP Source"]
   [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                            :map-style          default-stylesheet}
    [sources/source {:id   "cities"
                     :type "geojson"
                     :data "data/ne_110m_populated_places_simple.geojson"}
     [sources/layer {:id     "cities-circles"
                     :type   "circle"
                     :source "cities"
                     :paint  {:circle-radius       3
                              :circle-color        "#fff"
                              :circle-stroke-width 1
                              :circle-stroke-color "#a99"}}]]]

   [:p "Loads features from a GeoJSON source using an unauthenticated url."]
   [:p "Maplibre will perform the HTTP request, error handling, and parsing."]])

(defn heatmap-section []
  (let [points "data/earthquakes.geojson"]
    (fn []
      [:section
       [:h2 "Heatmap"]
       [cartoj/interactive-map {:initial-view-state {:longitude -129.9 :latitude 25 :zoom 0.8}
                                :map-style          default-stylesheet}
        [sources/source {:id "earthquakes" :type "geojson" :data points}
         [sources/layer {:id     "earthquake-heatmap"
                         :type   "heatmap"
                         :source "earthquakes"
                         :paint  {:heatmap-intensity 0.7
                                  :heatmap-color     ["interpolate" ["linear"] ["heatmap-density"]
                                                      0 "rgba(33,102,172,0)"
                                                      0.2 "rgb(103,169,207)"
                                                      0.4 "rgb(209,229,240)"
                                                      0.6 "rgb(253,219,199)"
                                                      0.9 "rgb(239,138,98)"
                                                      1 "rgb(178,24,43)"]
                                  :heatmap-radius    17
                                  :heatmap-opacity   0.7}}]]]
       [:p "Density heatmap of earthquakes, rendered from a GeoJSON file over HTTP."]])))

(defn limit-interactivity-section []
  (let [sfbay-bounds [[-123.4 37.2] [-121.3 38.5]]]
    (fn []
      [:section
       [:h2 "Limit Interactivity"]
       [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 8})
                                :map-style          default-stylesheet
                                :max-bounds         sfbay-bounds
                                :min-zoom           7
                                :max-zoom           12
                                :drag-rotate        false}
        [ctrl/navigation-control {:position "top-right"}]]
       [:p "Panning restricted to the San Francisco Bay Area. Zoom limited to 8-14. Rotation disabled."]])))

(defn dynamic-styling-section []
  (let [map-ref      (atom nil)
        layer-defs   [["water"                "fill-color"  "rgb(194, 200, 202)"]
                      ["park"                 "fill-color"  "rgb(230, 233, 229)"]
                      ["highway_major_inner"  "line-color"  "#fff"]
                      ["highway_major_casing" "line-color"  "rgb(213, 213, 213)"]
                      ["landuse_residential"  "fill-color"  "rgb(234, 234, 230)"]]
        defaults     (into {} (map (fn [[l p c]] [l {:paint p :default c}]) layer-defs))
        colors       (r/atom (into {} (map (fn [[l _ c]] [l c]) layer-defs)))
        random-color (fn []
                       (let [h (rand-int 360)
                             s (+ 40 (rand-int 41))
                             l (+ 40 (rand-int 31))]
                         (str "hsl(" h ", " s "%, " l "%)")))
        apply-all    (fn []
                       (when-let [^js m @map-ref]
                         (doseq [[layer {:keys [paint]}] defaults]
                           (.setPaintProperty m layer paint (get @colors layer)))))]
    (fn []
      [:section
       [:h2 "Dynamic Styling"]
       [cartoj/interactive-map {:initial-view-state (merge sf-coords {:zoom 9})
                                :map-style          default-stylesheet}
        [interop/reset-map-ref! map-ref]
        [ctrl/navigation-control {:position "top-right"}]]
       [:p "Change map layer styles programmatically:"]
       [:div {:style {:margin-top "12px"}}
        [:button {:on-click (fn [_]
                              (doseq [[layer _ _] layer-defs]
                                (swap! colors assoc layer (random-color)))
                              (apply-all))}
         "🎲 I'm Feeling Lucky"]
        [:button {:on-click (fn [_]
                              (doseq [[layer _ default] layer-defs]
                                (swap! colors assoc layer default))
                              (apply-all))}
         "↺ Reset Defaults"]]
       [:table
        [:thead
         [:tr
          [:th "Layer"]
          [:th "Paint"]
          [:th "Color"]]]
        [:tbody
         (doall
          (for [[layer {:keys [paint _default]}] defaults]
            ^{:key layer}
            [:tr
             [:td {:style {:font-family "monospace" :font-size "13px"}} layer]
             [:td {:style {:font-family "monospace" :font-size "13px"}} paint]
             [:td
              [:span {:style {:display          "inline-block"
                              :width            "14px"
                              :height           "14px"
                              :background-color (get @colors layer)
                              :border           "1px solid #999"
                              :margin-right     "6px"
                              :vertical-align   "middle"}}]
              [:code (get @colors layer)]]]))]]])))

(defn style-by-numeric-section []
  [:section
   [:h2 "Style by numeric properties"]
   [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                            :map-style          default-stylesheet}
    [ctrl/navigation-control {:position "top-right"}]
    [sources/source {:id   "cities"
                     :type "geojson"
                     :data "data/ne_110m_populated_places_simple.geojson"}
     [sources/layer {:id     "cities-circles"
                     :type   "circle"
                     :source "cities"
                     :paint  {:circle-radius       ["step" ["get" "pop_max"] 2
                                                    250000 3
                                                    1000000 4
                                                    2500000 6
                                                    5000000 8]
                              :circle-color        ["step" ["get" "pop_max"] "#ffffb2"
                                                    250000  "#fecc5c"
                                                    1000000  "#fd8d3c"
                                                    2500000 "#f03b20"
                                                    5000000 "#bd0026"]
                              :circle-opacity      0.8
                              :circle-stroke-width 1
                              :circle-stroke-color "#fff"}}]]]
   [:p
    "Using the " [:code "pop_max"] " property of the cities dataset, "
    "create a larger & darker red circle for cities with higher populations."]])

(defn side-by-side-section []
  (let [view-state (r/atom sf-coords)]
    (fn []
      (let [state @view-state]
        [:section
         [:h2 "Sync two map viewports."]
         [cartoj/interactive-map
          (merge state
                 {:map-style  default-stylesheet
                  :class-name "half-interactive-map"
                  :on-move    (fn [^js evt]
                                (reset! view-state (cartoj/view-state->clj (.-viewState evt))))})
          [ctrl/navigation-control {:position "top-right"}]]
         [cartoj/interactive-map
          (merge state
                 {:map-style  "https://tiles.openfreemap.org/styles/liberty"
                  :class-name "half-interactive-map"
                  :on-move    (fn [^js evt]
                                (reset! view-state (cartoj/view-state->clj (.-viewState evt))))})
          [ctrl/navigation-control {:position "top-right"}]]
         [:p "By registering an on-move handler, and subscribing to view-state,
              Clojurescript has full access to the map state and can sync the viewport."]]))))

(defn terrain-section []
  (let [terrain-source  {:id        "terrain-dem"
                         :type      "raster-dem"
                         :url       "https://demotiles.maplibre.org/terrain-tiles/tiles.json"
                         "tileSize" 256}
        hillshade-layer {:id     "hillshade"
                         :type   "hillshade"
                         :source "terrain-dem"
                         :paint  {:hillshade-exaggeration    0.25
                                  :hillshade-shadow-color    "#444"
                                  :hillshade-highlight-color "#ccc"
                                  :hillshade-accent-color    "#bbb"}}]
    (fn []
      [:section
       [:h2 "Terrain"]
       ;; demotiles.maplibre.org serves a single elevation tile around the Austrian Alps
       [cartoj/interactive-map {:initial-view-state {:longitude 11.396
                                                     :latitude  47.255
                                                     :zoom      11
                                                     :bearing   0
                                                     :pitch     55}
                                :map-style          "https://tiles.openfreemap.org/styles/liberty"
                                :terrain            {:source "terrain-dem" :exaggeration 1.0}
                                :sky                {"sky-color"         "#80ccff"
                                                     "sky-horizon-blend" 0.5
                                                     "horizon-color"     "#ccddff"
                                                     "horizon-fog-blend" 0.5
                                                     "fog-color"         "#fcf0dd"
                                                     "fog-ground-blend"  0.2}}
        [ctrl/navigation-control {:position "top-right"}]
        [ctrl/terrain-control {:position     "top-right"
                               :source       "terrain-dem"
                               :exaggeration 1.0}]
        [sources/source terrain-source
         [sources/layer hillshade-layer]]]
       [:p "3D terrain rendering with hillshade and sky atmosphere."]])))

(defn drawing-section []
  (let [geometry  (r/atom nil)
        on-render (fn [_evt ^js draw]
                    (let [^js fc (.getAll draw)
                          ^js fs (.-features fc)
                          ^js f  (when (pos? (.-length fs)) (aget fs 0))]
                      (reset! geometry
                              (when f
                                (js->clj (.-geometry f) :keywordize-keys true)))))]
    (fn []
      [:section
       [:h2 "Drawing Geometries"]
       [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                                :map-style          default-stylesheet}
        [draw/draw-control {:position  "top-left"
                            :on-render on-render}]]
       [:div.geometry-panel
        [:p "Use the draw tools to create a point, linestring, or polygon.
             The EDN representation of the geometry updates live as you click each vertex;
             double-click to complete."]
        (if (nil? @geometry)
          [:p {:style {:color "#888"}} "No geometry yet — draw on the map."]
          [:pre (with-out-str (cljs.pprint/pprint @geometry))])]
       [:br]])))

(defn render-counter-section
  "Probe whether props->js identity churn causes downstream work in
  react-map-gl. The CLJS props are held structurally stable across
  parent re-renders, but props->js still produces fresh JS object
  identity for the nested values (:initial-view-state, :style,
  :interactive-layer-ids) on every render. The counters below show
  how react-map-gl reacts.

  Deviates from the form-2 convention used elsewhere because we need
  unmount cleanup for the auto-tick interval; r/with-let's (finally)
  handles that."
  []
  (r/with-let
    [;; Plain atoms — incremented without driving Reagent reactivity.
     ;; Values are sampled at render time via deref.
     parent-renders     (atom 0)
     on-load-count      (atom 0)
     on-style-data-count (atom 0)
     on-render-count    (atom 0)
     on-idle-count      (atom 0)

     ;; r/atoms — drive re-renders.
     tick       (r/atom 0)
     auto-tick? (r/atom false)

     ;; Plain atom for cleanup — not displayed.
     timer-id (atom nil)

     ;; Stable callback identities so the prop value of each :on-*
     ;; is the same JS fn across renders.
     handle-load       (fn [_] (swap! on-load-count inc))
     handle-style-data (fn [_] (swap! on-style-data-count inc))
     handle-render     (fn [_] (swap! on-render-count inc))
     handle-idle       (fn [_] (swap! on-idle-count inc))

     ;; CLJS prop map — structurally identical across renders. The
     ;; nested maps + vector here are what props->js will repeatedly
     ;; convert into fresh JS objects/arrays.
     stable-props {:initial-view-state    (merge sf-coords {:zoom 9})
                   :map-style             default-stylesheet
                   :interactive-layer-ids ["probe-a" "probe-b"]
                   :style                 {:width "100%" :height "100%"}
                   :on-load               handle-load
                   :on-style-data         handle-style-data
                   :on-render             handle-render
                   :on-idle               handle-idle}

     start-auto!
     (fn []
       (when-not @timer-id
         (reset! auto-tick? true)
         (reset! timer-id (js/setInterval #(swap! tick inc) 200))))

     stop-auto!
     (fn []
       (when-let [id @timer-id]
         (js/clearInterval id)
         (reset! timer-id nil))
       (reset! auto-tick? false))

     reset-counts!
     (fn []
       (reset! parent-renders 0)
       (reset! on-load-count 0)
       (reset! on-style-data-count 0)
       (reset! on-render-count 0)
       (reset! on-idle-count 0)
       (reset! tick 0))]

    ;; Body — runs on every render. Deref tick to subscribe to it,
    ;; then bump the (plain) parent-renders counter.
    @tick
    (swap! parent-renders inc)
    [:section
     [:h2 "Render Counter (props identity probe)"]
     [:p "Forces parent re-renders while the CLJS-side props passed to "
      [:code "cartoj/interactive-map"]
      " stay structurally identical. JS-side, "
      [:code "props->js"]
      " still produces fresh JS object identity for the nested values ("
      [:code ":initial-view-state"] ", "
      [:code ":style"] ", "
      [:code ":interactive-layer-ids"]
      ") on every render. If react-map-gl is reacting to that identity churn, "
      "the event counters below should keep climbing as you tick."]

     [cartoj/interactive-map stable-props
      [ctrl/navigation-control {:position "top-right"}]]

     [:div.control-panel {:style {:margin "12px 0"}}
      [:button {:on-click #(swap! tick inc)}
       "Force re-render"]
      [:button {:on-click #(if @auto-tick? (stop-auto!) (start-auto!))}
       (if @auto-tick? "Stop auto-tick" "Auto-tick every 200 ms")]
      [:button {:on-click reset-counts!}
       "Reset counts"]]

     [:table
      [:tbody
       [:tr [:th "Tick"]                    [:td @tick]]
       [:tr [:th "Parent renders"]          [:td @parent-renders]]
       [:tr [:th [:code "on-load"]]         [:td @on-load-count]]
       [:tr [:th [:code "on-style-data"]]   [:td @on-style-data-count]]
       [:tr [:th [:code "on-render"]]       [:td @on-render-count]]
       [:tr [:th [:code "on-idle"]]         [:td @on-idle-count]]]]

     [:p {:style {:color "#666" :font-size "0.9em"}}
      "Interpretation: "
      [:code "on-load"] " should fire once. "
      [:code "on-style-data"]
      " is the load-bearing counter — if it keeps climbing on every forced "
      "re-render, identity churn is triggering style reloads. If it plateaus "
      "after the initial fetch, react-map-gl is comparing more carefully than "
      "reference identity. "
      [:code "on-render"]
      " fires per painted frame; growth while the map is visually static "
      "signals wasted re-paint work. "
      [:code "on-idle"] " ticks when motion stops."]]

    (finally (stop-auto!))))

(def tabs
  (sorted-map
   :barebones         {:title   "Barebones"
                       :section barebones-section}
   :basemaps         {:title   "Basemaps"
                      :section basemaps-section}
   :on-click-event   {:title   "Handle :on-click"
                      :section on-click-event-section}
   :on-move-event   {:title   "Handle :on-move"
                     :section on-move-event-section}
   :on-mouse-move-event {:title   "Handle :on-mouse-move"
                         :section on-mouse-move-event-section}
   :cluster          {:title   "Cluster"
                      :section cluster-section}
   :controls         {:title   "Controls"
                      :section controls-section}
   :drawing          {:title   "Drawing Geometries"
                      :section drawing-section}
   :dynamic-style    {:title   "Dynamic Styling"
                      :section dynamic-styling-section}
   :flyto            {:title   "Fly To Location"
                      :section flyto-section}
   :geocoder         {:title   "Geocoder"
                      :section geocoder-section}
   :geojson-http     {:title   "GeoJSON HTTP (maplibre)"
                      :section geojson-http-section}
   :geojson-manual   {:title   "GeoJSON HTTP (manual)"
                      :section geojson-manual-http-section}
   :heatmap          {:title   "Heatmap"
                      :section heatmap-section}
   :labels           {:title   "Labeling"
                      :section label-section}
   :layer-interact   {:title   "Layer Interactivity"
                      :section layer-interactivity-section}
   :layer-switcher   {:title   "Layer Switcher"
                      :section layer-switcher-section}
   :limit-interact   {:title   "Limit Viewport"
                      :section limit-interactivity-section}
   :overlays         {:title   "Markers and Popups"
                      :section overlay-section}
   :reframe          {:title   "Reframe integration"
                      :section reframe-section}
   :render-counter   {:title   "Render Counter"
                      :section render-counter-section}
   :side-by-side     {:title   "Side by Side"
                      :section side-by-side-section}
   :sources          {:title   "GeoJSON Features"
                      :section point-features-section}
   :style-by-numeric {:title   "Style by numeric"
                      :section style-by-numeric-section}
   :terrain          {:title   "Terrain"
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
             :on-click-event [code-block (code-string on-click-event-section)]
             :on-mouse-move-event [code-block (code-string on-mouse-move-event-section)]
             :on-move-event [code-block (code-string on-move-event-section)]
             :controls    [code-block (code-string controls-section)]
             :render-counter [code-block (code-string render-counter-section)]
             :reframe [code-block (code-string reframe-section)]
             :overlays    [code-block (code-string overlay-section)]
             :geocoder    [code-block (code-string geocoder-section)]
             :flyto       [code-block (code-string flyto-section)]
             :sources          [code-block (code-string point-features-section)]
             :layer-switcher   [code-block (code-string layer-switcher-section)]
             :layer-interact   [code-block (code-string layer-interactivity-section)]
             :style-by-numeric [code-block (code-string style-by-numeric-section)]
             :geojson-http     [code-block (code-string geojson-http-section)]
             :geojson-manual   [code-block (code-string geojson-manual-http-section)]
             :heatmap          [code-block (code-string heatmap-section)]
             :cluster          [code-block (code-string cluster-section)]
             :limit-interact   [code-block (code-string limit-interactivity-section)]
             :dynamic-style    [code-block (code-string dynamic-styling-section)]
             :side-by-side     [code-block (code-string side-by-side-section)]
             :terrain          [code-block (code-string terrain-section)]
             :drawing          [code-block (code-string drawing-section)]
             :labels         [code-block (code-string label-section)]
             :basemaps       [code-block (code-string basemaps-section)]
             :barebones       [code-block (code-string barebones-section)]
             [:code "placeholder"])]]))

(defn app []
  [:div
   [:header.demo-header
    [:a {:style {:float "right"} :href "https://perrygeo.github.io/cartoj"} "Home"]
    [:h1 "Cartoj Examples"]]
   [:div.demo-layout
    [sidebar]
    [main-panel]]])

(defonce root (atom nil))

(defn ^:dev/after-load re-render []
  (.render ^js @root (r/as-element [app])))

(defn init []
  (rf/dispatch-sync [::cartoj-rf/set-view-state sf-coords])
  (reset! root (rdom-client/create-root (js/document.getElementById "app")))
  (.render ^js @root (r/as-element [app])))

(comment
  ;; key conversion behavior
  (js/console.log
   (clj->js {:beforeId true}) ; #js {:beforeId true} -> Object { beforeId: true } 
   (clj->js {:before-id true}) ; {:before-id true} -> Object { "before-id": true }
   (clj->js {"beforeId" true}) ; {"beforeId" true} -> Object { beforeId: true }
   )
  (re-render)
  (init)
  (-> js/document (.getElementById "app") (.-innerHTML))
  (js/alert (str "Hello from the REPL, in namespace " (namespace ::x))))

(defn on-click-example
  "A Form-2 style component, tracks map click coordinates in a reagant atom."
  []
  (let [last-point    (r/atom nil)
        click-handler (fn [^js e]
                        (reset! last-point (interop/coords-from-evt e)))]
    (fn []
      [:section
       [:h2 "On click event"]
       [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                                :map-style          "https://tiles.openfreemap.org/styles/positron"
                                :on-click           click-handler}
        [ctrl/navigation-control {:position "top-right"}]]
       [:table {:style {:width "250px"}}
        [:tbody
         [:tr
          [:th "Longitude"]
          [:td (if-let [lng (:longitude @last-point)]
                 (.toFixed lng 4) "-")]]
         [:tr
          [:th "Latitude"]
          [:td (if-let [lat (:latitude @last-point)]
                 (.toFixed lat 4) "-")]]]]])))
