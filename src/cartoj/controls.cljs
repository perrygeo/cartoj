(ns cartoj.controls
  "Reagent wrappers around react-map-gl control components.

  All controls accept kebab-case keyword props and an optional :position key
  (one of \"top-left\", \"top-right\", \"bottom-left\", \"bottom-right\").

  Usage:
    [controls/navigation-control {:position \"top-right\"}]
    [controls/geolocate-control  {:position \"bottom-right\" :track-user-location true}]
    [controls/fullscreen-control {:position \"top-left\"}]
    [controls/scale-control      {:max-width 100 :unit \"metric\"}]
    [controls/attribution-control {:compact true}]"
  (:require ["react-map-gl/maplibre" :refer [NavigationControl
                                              GeolocateControl
                                              FullscreenControl
                                              ScaleControl
                                              AttributionControl
                                              TerrainControl]]
            [cartoj.props :as props]))

(defn- make-control
  "Generic factory: converts CLJS props to JS and returns [:r> Component props]."
  [Component args]
  (let [[prop-map children] (props/props-and-children args)
        js-props (props/props->js (or prop-map {}))]
    (into [:r> Component js-props] children)))

;; ---------------------------------------------------------------------------
;; Individual control components

(defn navigation-control
  "Zoom in/out + compass bearing reset control.
  Key props: :position, :show-compass, :show-zoom, :visualize-pitch."
  [& args]
  (make-control NavigationControl args))

(defn geolocate-control
  "Geolocation control.
  Key props: :position, :track-user-location, :show-user-heading,
             :show-accuracy-circle, :show-user-location."
  [& args]
  (make-control GeolocateControl args))

(defn fullscreen-control
  "Fullscreen toggle control.
  Key props: :position, :container (DOM element to go fullscreen)."
  [& args]
  (make-control FullscreenControl args))

(defn scale-control
  "Distance scale bar control.
  Key props: :position, :max-width (px), :unit (\"metric\" | \"imperial\" | \"nautical\")."
  [& args]
  (make-control ScaleControl args))

(defn attribution-control
  "Map attribution control.
  Key props: :position, :compact, :custom-attribution."
  [& args]
  (make-control AttributionControl args))

(defn terrain-control
  "Terrain toggle control.
  Key props: :position, :source, :exaggeration."
  [& args]
  (make-control TerrainControl args))
