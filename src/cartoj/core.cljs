(ns cartoj.core
  "Map component and view-state helpers for cartoj.

  Wraps the react-map-gl/maplibre Map, Source, and Layer as a Reagent component.

  Usage:
    [cartoj/interactive-map
        {:initial-view-state {:longitude 0 :latitude 16 :zoom 1}
         :map-style \"https://tiles.openfreemap.org/styles/bright\"}]"

  (:require ["react-map-gl/maplibre" :refer [Map Source Layer]]
            [cartoj.props :as props]))

(defn interactive-map
  "Reagent component wrapping react-map-gl Map.

  Props are a CLJS map with kebab-case keyword keys; they are converted to a
  camelCase JS object before being passed to the underlying React component.
  Callback values are passed through unchanged. Children are appended after
  the props in the hiccup vector.

  :class-name is intercepted and applied to a wrapper div, because the
  underlying react-map-gl Map only forwards :id, :ref, and :style to its
  container element. When omitted, defaults to \"cartoj-interactive-map\".

  Key props:
    :id                   — string, unique map instance id
    :map-style            — string URL or MapLibre style spec (required)
    :initial-view-state   — {:longitude :latitude :zoom :bearing :pitch :padding}
    :view-state           — controlled view state (use with re-frame :sync?)
    :style                — map of CSS properties for the container
    :projection           — \"mercator\" | \"globe\" | projection spec
    :style-diffing        — boolean, enable diffing on style change (default true)
    :interactive-layer-ids — vector of layer ids for pointer queries
    :cursor               — CSS cursor string
    :light                — light spec map
    :terrain              — terrain spec map
    :sky                  — sky spec map
    :gl                   — external WebGL context
    :reuse-maps           — boolean, reuse existing Map instances

  Mouse events:
    :on-click :on-dbl-click :on-mouse-down :on-mouse-up
    :on-mouse-over :on-mouse-move :on-mouse-enter :on-mouse-leave
    :on-mouse-out :on-context-menu

  Touch events:
    :on-touch-start :on-touch-end :on-touch-move :on-touch-cancel

  View-state events:
    :on-move-start :on-move :on-move-end
    :on-drag-start :on-drag :on-drag-end
    :on-zoom-start :on-zoom :on-zoom-end
    :on-rotate-start :on-rotate :on-rotate-end
    :on-pitch-start :on-pitch :on-pitch-end

  Other events:
    :on-wheel :on-box-zoom-start :on-box-zoom-end :on-box-zoom-cancel

  Lifecycle & data events:
    :on-load :on-render :on-idle :on-error :on-remove :on-resize
    :on-data :on-style-data :on-source-data"
  [& args]
  (let [[prop-map children] (props/props-and-children args)
        prop-map            (or prop-map {})
        class-name          (or (:class-name prop-map) "cartoj-interactive-map")
        js-props            (props/props->js (dissoc prop-map :class-name))
        map-el              (into [:r> Map js-props] children)]
    (if class-name
      [:div {:class class-name} map-el]
      map-el)))

; (ns cartoj.sources
;   "Source and Layer components for cartoj.

;   Usage:
;     [sources/source {:id \"my-source\" :type \"geojson\" :data \"/data.geojson\"}
;      [sources/layer {:id \"my-layer\" :type \"circle\" :source \"my-source\"
;                      :paint {:circle-radius 6 :circle-color \"#e00\"}}]]"
;   (:require ["react-map-gl/maplibre" :refer [Source Layer]]
;             [cartoj.props :as props]))

(defn source
  "Reagent component wrapping react-map-gl Source.

  Required props:
    :id    – unique string identifier
    :type  – one of \"geojson\", \"vector\", \"raster\", \"raster-dem\",
              \"image\", \"video\"

  Additional props depend on type (e.g. :data for geojson, :tiles for vector).
  Children should be Layer components."
  [& args]
  (let [[prop-map children] (props/props-and-children args)
        js-props            (props/props->js (or prop-map {}))]
    (into [:r> Source js-props] children)))

(defn layer
  "Reagent component wrapping react-map-gl Layer.

  Required props:
    :id      – unique string identifier
    :type    – maplibre layer type (\"fill\", \"line\", \"circle\", \"symbol\", etc.)
    :source  – id of the source this layer reads from

  Optional props include :paint, :layout, :filter, :min-zoom, :max-zoom,
  :source-layer, :before-id.

  All layer props go directly to MapLibre's map.addLayer() which expects
  MapLibre style spec keys (kebab-case). clj->js is used instead of
  cartoj.props/props->js because clj->js preserves keyword names as-is:
  :source-layer → \"source-layer\", :circle-radius → \"circle-radius\",
  :beforeId → \"beforeId\"."
  [& args]
  (let [[prop-map children] (props/props-and-children args)
        js-props            (clj->js (or prop-map {}))]
    (into [:r> Layer js-props] children)))
