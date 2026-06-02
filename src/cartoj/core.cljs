(ns cartoj.core
  "Map component and view-state helpers for cartoj.

  Wraps the react-map-gl/maplibre Map component as a Reagent component.

  Usage (uncontrolled):
    [core/map {:initial-view-state {:longitude -122.4 :latitude 37.8 :zoom 14}
               :map-style \"https://demotiles.maplibre.org/style.json\"
               :style {:width \"100%\" :height \"400px\"}}]

  Usage (controlled):
    [core/map (merge @view-state
                     {:on-move #(reset! view-state (core/view-state->clj (.-viewState %)))
                      :map-style \"...\"})
     child1 child2]"
  (:require ["react-map-gl/maplibre" :refer [Map]]
            [cartoj.props :as props]
            [cartoj.pmtiles]))

(defn interactive-map
  "Reagent component wrapping react-map-gl Map.

  Props are a CLJS map with kebab-case keyword keys; they are converted to a
  camelCase JS object before being passed to the underlying React component.
  Callback values (e.g. :on-click, :on-move) are passed through unchanged.
  Children are appended after the props in the hiccup vector.

  :class-name is intercepted and applied to a wrapper div, because the
  underlying react-map-gl Map only forwards :id, :ref, and :style to its
  container element."
  [& args]
  (let [[prop-map children] (props/props-and-children args)
        prop-map  (or prop-map {})
        class-name (or (:class-name prop-map) "cartoj-interactive-map")
        js-props  (props/props->js (dissoc prop-map :class-name))
        map-el    (into [:r> Map js-props] children)]
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
