(ns cartoj.interop
  "Interop helpers for advanced react-map-gl usage.

  Provides:
    - `with-map`    — render a child with access to the MapRef (useMap shim)
    - `map-provider` — multi-map context (wraps MapProvider)
    - `use-map`     — raw useMap hook reference (call inside :f> components only)
    - `use-control` — raw useControl hook reference (call inside :f> components only)

  IMPORTANT: `use-map` and `use-control` are React hooks.  They MUST be called
  inside a React functional component.  In Reagent, use them inside a component
  rendered with `:f>`, or use the `with-map` helper below.

  Usage — with-map:
    [interop/with-map
     (fn [map-instance]
       ;; map-instance is the maplibre Map (resolved via .getMap() on the MapRef)
       [:button {:on-click #(.flyTo map-instance (clj->js {:center [-122 37] :zoom 12}))}
        \"Fly!\"])]

  Usage — map-provider + use-map:
    [interop/map-provider {:id \"main\"}
     [:f> (fn []
            (let [{:keys [current]} (interop/use-map)]
              ;; current is the MapRef for the nearest ancestor Map
              [:div (str \"zoom: \" (.. current -transform -zoom))]))]]"
  (:require ["react-map-gl/maplibre" :refer [useMap useControl MapProvider]]
            [reagent.core :as r]
            [cartoj.props :as props]))

;; ---------------------------------------------------------------------------
;; Raw hook exports
;; These are plain JS function references.  Calling them outside a React
;; functional component will throw a React hooks invariant error.

(def use-map
  "The useMap hook from react-map-gl.
  Returns a map of {id -> MapRef} for all ancestor Maps (or {:current MapRef}
  for the nearest ancestor).  Must be called inside a functional component."
  useMap)

(def use-control
  "The useControl hook from react-map-gl.
  Mounts a native maplibre/mapbox IControl imperatively.
  Signature: (use-control create-fn options)
  Must be called inside a functional component."
  useControl)

(defn with-map
  "Render children with access to the maplibre Map instance.

  `render-fn` is a function that receives the maplibre Map instance (or nil if
  no Map is mounted yet) and returns a Reagent hiccup vector.

  This helper wraps useMap inside a functional component (:f>) so it can be
  used from ordinary Reagent components without hooks friction."
  [render-fn]
  [:f> (fn []
         (let [map-ctx (useMap)
               map-ref (.-current map-ctx)]
           (r/as-element
            (render-fn (when map-ref (.getMap map-ref))))))])

(defn reset-map-ref!
  "Reset the given map-ref atom, so that it's subscribers
  have access to imperative maplibre commands, like flyTo, etc.
  used from ordinary Reagent components without hooks friction.

  Wraps the most commonly-used `with-map` pattern for convenience."
  [map-ref]
  (with-map
    (fn [m] (reset! map-ref m) [:<>])))

(defn map-provider
  "Reagent component wrapping react-map-gl MapProvider.

  Used when you have multiple Map components and want to access them by id
  via use-map.

  Optional props: none required; each child Map should have an :id prop."
  [& args]
  (let [[prop-map children] (props/props-and-children args)
        js-props (props/props->js (or prop-map {}))]
    (into [:r> MapProvider js-props] children)))

