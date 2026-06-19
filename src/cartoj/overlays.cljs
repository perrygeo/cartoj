(ns cartoj.overlays
  "Marker and Popup components for cartoj.

  Usage:
    [overlays/marker {:longitude -122.4 :latitude 37.8}
     [:div \"custom pin\"]]

    [overlays/popup {:longitude -122.4 :latitude 37.8
                     :on-close #(reset! show? false)}
     [:p \"Hello, world!\"]]"
  (:require ["react-map-gl/maplibre" :refer [Marker Popup]]
            [cartoj.props :as props]))

;; ---------------------------------------------------------------------------
;; Marker

(defn marker
  "Reagent component wrapping react-map-gl Marker.

  Required props:
    :longitude  — number
    :latitude   — number

  Optional props:
    :offset             — [x y] pixel offset
    :anchor             — position anchor (see MapLibre docs)
    :color              — string, default marker color (default \"#3FB1CE\")
    :scale              — number, scale factor (default 1)
    :draggable          — boolean
    :click-tolerance    — number, max pixel shift for click detection
    :rotation           — number, rotation in degrees
    :rotation-alignment — \"map\" | \"viewport\" | \"auto\"
    :pitch-alignment    — \"map\" | \"viewport\" | \"auto\"
    :opacity            — string or number, CSS opacity
    :popup              — PopupInstance, attach a popup to the marker
    :element            — HTMLElement, custom DOM element
    :style              — map of CSS properties
    :class-name         — string, CSS class

  Callback props:
    :on-click      — (fn [^js evt])
    :on-drag-start — (fn [^js evt])
    :on-drag       — (fn [^js evt])
    :on-drag-end   — (fn [^js evt])

  Children replace the default pin icon."
  [& args]
  (let [[prop-map children] (props/props-and-children args)
        js-props (props/props->js (or prop-map {}))]
    (into [:r> Marker js-props] children)))

;; ---------------------------------------------------------------------------
;; Popup

(defn popup
  "Reagent component wrapping react-map-gl Popup.

  Required props:
    :longitude  — number
    :latitude   — number

  Optional props:
    :offset                   — [x y] pixel offset
    :anchor                   — position anchor (see MapLibre docs)
    :close-button             — boolean (default true)
    :close-on-click           — boolean (default true)
    :close-on-move            — boolean (default false)
    :focus-after-open         — boolean (default true)
    :max-width                — string, CSS max-width (default \"240px\")
    :subpixel-positioning     — boolean (default false)
    :location-occluded-opacity — number or string
    :style                    — map of CSS properties
    :class-name               — string, CSS class

  Callback props:
    :on-open  — (fn [^js evt])
    :on-close — (fn [^js evt])

  Children are rendered inside the popup."
  [& args]
  (let [[prop-map children] (props/props-and-children args)
        js-props (props/props->js (or prop-map {}))]
    (into [:r> Popup js-props] children)))
