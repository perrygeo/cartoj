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
    :longitude  – number
    :latitude   – number

  Optional props include :offset, :anchor, :draggable, :rotation,
  :on-drag-start, :on-drag, :on-drag-end, :on-click, :style, :class-name.

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
    :longitude  – number
    :latitude   – number

  Optional props include :offset, :anchor, :close-button, :close-on-click,
  :close-on-move, :focus-after-open, :on-close, :on-open, :style, :class-name.

  Children are rendered inside the popup."
  [& args]
  (let [[prop-map children] (props/props-and-children args)
        js-props (props/props->js (or prop-map {}))]
    (into [:r> Popup js-props] children)))
