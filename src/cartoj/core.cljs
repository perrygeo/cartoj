(ns cartoj.core
  "Map component and view-state helpers for cartoj.

  Wraps the react-map-gl/maplibre Map component as a Reagent component.

  Usage:
    [cartoj/interactive-map
        {:initial-view-state {:longitude 0 :latitude 16 :zoom 1}
         :map-style \"https://tiles.openfreemap.org/styles/bright\"}]"

  (:require ["react-map-gl/maplibre" :refer [Map]]
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

(def ^:private view-state-keys
  [:longitude :latitude :zoom :bearing :pitch :padding])

(defn view-state->js
  "Convert a CLJS view-state map to a plain JS object.
  Converts keyword keys to camelCase strings."
  [vs]
  (props/props->js vs))

(defn view-state->clj
  "Convert a JS viewState object (from a react-map-gl onMove event) to a
  CLJS map with keyword keys.

  Only the standard view-state keys are extracted:
  :longitude :latitude :zoom :bearing :pitch :padding"
  [js-vs]
  (reduce (fn [acc k]
            (let [js-key (props/kebab->camel k)
                  v      (aget js-vs js-key)]
              (if (some? v)
                (assoc acc k v)
                acc)))
          {}
          view-state-keys))
