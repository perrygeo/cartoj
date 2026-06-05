(ns cartoj.sources
  "Source and Layer components for cartoj.

  Usage:
    [sources/source {:id \"my-source\" :type \"geojson\" :data \"/data.geojson\"}
     [sources/layer {:id \"my-layer\" :type \"circle\" :source \"my-source\"
                     :paint {:circle-radius 6 :circle-color \"#e00\"}}]]"
  (:require ["react-map-gl/maplibre" :refer [Source Layer]]
            [cartoj.props :as props]))

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
        js-props (props/props->js (or prop-map {}))]
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
        js-props (clj->js (or prop-map {}))]
    (into [:r> Layer js-props] children)))
