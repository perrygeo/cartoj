(ns cartoj.props
  "Utilities for converting ClojureScript prop maps into JavaScript objects
  suitable for passing to react-map-gl components.

  Convention:
  - Incoming: CLJS map with kebab-case keyword keys (or string keys for pass-through)
  - Outgoing: JS object with camelCase string keys
  - Callback values (fns) are passed through unchanged
  - Nested CLJS maps on JS/CSS props (:initial-view-state, :style, :padding,
    :bounds, :fit-bounds-options) are converted recursively with camelCase keys.
  - Nested CLJS maps on MapLibre style spec props (:paint, :layout, :filter)
    are converted recursively with kebab-case keys (e.g. :circle-radius → \"circle-radius\").
  - All other values are passed through as-is (numbers, strings, booleans)"
  (:require [clojure.string :as str]
            [clojure.set]))

(defn kebab->camel
  "Convert a kebab-case keyword (or camelCase string) to a camelCase string.

  Examples:
    :foo              => \"foo\"
    :foo-bar          => \"fooBar\"
    :initial-view-state => \"initialViewState\"
    \"onMove\"        => \"onMove\"  (string passed through unchanged)"
  [k]
  (if (string? k)
    k
    (let [s (name k)
          parts (str/split s #"-")]
      (str (first parts)
           (str/join (map str/capitalize (rest parts)))))))

;; Props that contain nested CLJS data maps that must be converted to JS
;; Keys whose nested map values use camelCase keys (JS/CSS conventions).

(def ^:private camel-convert-keys
  #{:initial-view-state
    :view-state
    :style
    :bounds
    :max-bounds
    :padding
    :fit-bounds-options
    :interactive-layer-ids})

;; Keys whose nested map values use kebab-case keys (MapLibre style spec).

(def ^:private maplibre-spec-keys
  #{:paint
    :layout
    :filter})

(def ^:private deep-convert-keys
  (clojure.set/union camel-convert-keys maplibre-spec-keys))

(defn- map->js-camel
  "Recursively convert a CLJS map to a JS object with camelCase keys.
  Used for JS/CSS nested props (:style, :initial-view-state, etc.)."
  [m]
  (let [obj (js-obj)]
    (doseq [[k v] m]
      (let [js-key (kebab->camel k)
            js-val (cond
                     (and (map? v) (not (satisfies? IRecord v))) (map->js-camel v)
                     (vector? v) (clj->js v)
                     :else v)]
        (aset obj js-key js-val)))
    obj))

(defn- map->js-kebab
  "Recursively convert a CLJS map to a JS object preserving kebab-case keys.
  Used for MapLibre style spec props (:paint, :layout) where property names
  like 'circle-radius' and 'icon-image' must remain kebab-case."
  [m]
  (let [obj (js-obj)]
    (doseq [[k v] m]
      (let [js-key (name k)
            js-val (cond
                     (and (map? v) (not (satisfies? IRecord v))) (map->js-kebab v)
                     (vector? v) (clj->js v)
                     :else v)]
        (aset obj js-key js-val)))
    obj))

(defn- convert-value
  "Convert a prop value to its JS equivalent.
  - Maps on camel-convert keys are recursively converted with camelCase keys
  - Maps on maplibre-spec keys are recursively converted with kebab-case keys
  - Functions pass through unchanged
  - All other values pass through unchanged"
  [k v]
  (cond
    (and (contains? camel-convert-keys k)    (map? v)) (map->js-camel v)
    (and (contains? maplibre-spec-keys k)    (map? v)) (map->js-kebab v)
    (contains? deep-convert-keys k)                    (clj->js v)
    :else v))

(defn props->js
  "Convert a ClojureScript props map to a JS object for react-map-gl.

  - Keyword keys are converted from kebab-case to camelCase
  - String keys are used as-is (no conversion)
  - Values for known nested-data keys (:initial-view-state, :style, etc.)
    are converted with clj->js
  - All other values (including callback fns) pass through unchanged"
  [props]
  (let [obj (js-obj)]
    (doseq [[k v] props]
      (let [js-key (kebab->camel k)
            js-val (convert-value k v)]
        (aset obj js-key js-val)))
    obj))

(defn props-and-children
  "Split Reagent component args into [props children].
  If the first arg is nil or a map, it is the props; otherwise props is {}."
  [args]
  (if (or (nil? (first args)) (map? (first args)))
    [(first args) (rest args)]
    [{} args]))
