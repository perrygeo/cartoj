(ns cartoj.draw
  "MapboxDraw control component using react-map-gl's useControl hook.

  Usage:
    [draw/draw-control {:position \"top-left\"
                         :on-create (fn [^js evt] ...)
                         :on-update (fn [^js evt] ...)
                         :on-delete (fn [^js evt] ...)}]"
  (:require ["@mapbox/mapbox-gl-draw" :as draw-module]
            [cartoj.interop :as interop]
            [cartoj.props :as props]
            [clojure.string :as str]))

(def ^:private MapboxDraw (.-default draw-module))

;; ---------------------------------------------------------------------------
;; MapLibre 3+ compatibility shim
;;
;; @mapbox/mapbox-gl-draw emits its DOM with the legacy `mapboxgl-ctrl` /
;; `mapboxgl-ctrl-group` classes.  MapLibre 3+ dropped those aliases and ships
;; only `.maplibregl-*` selectors, so two things break:
;;   1. `.maplibregl-ctrl-top-left` sets `pointer-events: none` on the slot,
;;      and the override `.maplibregl-ctrl { pointer-events: auto }` never
;;      matches → clicks fall through to the canvas.
;;   2. The white pill background from `.maplibregl-ctrl-group` is missing,
;;      so the buttons render against whatever CSS framework styles `<button>`.
;;
;; The CSS below re-applies MapLibre's own control rules to the legacy class
;; names so the draw toolbar looks and behaves like a native MapLibre control.

(def compat-css
  ".mapboxgl-ctrl { clear: both; pointer-events: auto; transform: translate(0); }
.mapboxgl-ctrl-group { background: #fff; border-radius: 4px; }
.mapboxgl-ctrl-group:not(:empty) { box-shadow: 0 0 0 2px rgba(0,0,0,0.1); }
.mapboxgl-ctrl-group button {
  background-color: transparent;
  border: 0;
  box-sizing: border-box;
  cursor: pointer;
  display: block;
  height: 29px;
  outline: none;
  padding: 0;
  width: 29px;
}
.mapboxgl-ctrl-group button + button { border-top: 1px solid #ddd; }
.mapboxgl-ctrl-group button:hover { background-color: rgba(0,0,0,0.05); }
")

(def ^:private style-element-id "cartoj-draw-compat-styles")

(defn- inject-compat-styles!
  "Append the compat CSS as a <style> element in <head>, once.  Idempotent:
  a second call is a no-op (checked via the known element id)."
  []
  (when-not (.getElementById js/document style-element-id)
    (let [el (.createElement js/document "style")]
      (set! (.-id el) style-element-id)
      (set! (.-textContent el) compat-css)
      (.appendChild (.-head js/document) el))))

(when (exists? js/document)
  (inject-compat-styles!))

;; ---------------------------------------------------------------------------
;; MapLibre-compatible default theme
;;
;; mapbox-gl-draw's default theme has a `line-dasharray` paint expression on
;; `gl-draw-lines` whose `case` branches return bare arrays:
;;
;;   ['case', ['==', ['get', 'active'], 'true'], [0.2, 2], [2, 0]]
;;
;; MapLibre 5+ refuses to parse this — it reads `[0.2, 2]` as a function call
;; where `0.2` is the function name — and the gl-draw-lines layer is silently
;; dropped on add.  The visible result: line edges and polygon outlines do not
;; render, even though vertices (rendered by other layers) do.
;;
;; The fix is to wrap each array branch in `['literal' ...]`.  Everything else
;; is a structural copy of mapbox-gl-draw's `src/lib/theme.js`.

(def ^:private blue "#3bb2d0")
(def ^:private orange "#fbb03b")
(def ^:private white "#fff")

(def default-styles
  [{"id" "gl-draw-polygon-fill"
    "type" "fill"
    "filter" ["all" ["==" "$type" "Polygon"]]
    "paint" {"fill-color" ["case" ["==" ["get" "active"] "true"] orange blue]
             "fill-opacity" 0.1}}
   {"id" "gl-draw-lines"
    "type" "line"
    "filter" ["any" ["==" "$type" "LineString"] ["==" "$type" "Polygon"]]
    "layout" {"line-cap" "round" "line-join" "round"}
    "paint" {"line-color" ["case" ["==" ["get" "active"] "true"] orange blue]
             "line-dasharray" ["case"
                               ["==" ["get" "active"] "true"] ["literal" [0.2 2]]
                               ["literal" [2 0]]]
             "line-width" 2}}
   {"id" "gl-draw-point-outer"
    "type" "circle"
    "filter" ["all" ["==" "$type" "Point"] ["==" "meta" "feature"]]
    "paint" {"circle-radius" ["case" ["==" ["get" "active"] "true"] 7 5]
             "circle-color" white}}
   {"id" "gl-draw-point-inner"
    "type" "circle"
    "filter" ["all" ["==" "$type" "Point"] ["==" "meta" "feature"]]
    "paint" {"circle-radius" ["case" ["==" ["get" "active"] "true"] 5 3]
             "circle-color" ["case" ["==" ["get" "active"] "true"] orange blue]}}
   {"id" "gl-draw-vertex-outer"
    "type" "circle"
    "filter" ["all"
              ["==" "$type" "Point"]
              ["==" "meta" "vertex"]
              ["!=" "mode" "simple_select"]]
    "paint" {"circle-radius" ["case" ["==" ["get" "active"] "true"] 7 5]
             "circle-color" white}}
   {"id" "gl-draw-vertex-inner"
    "type" "circle"
    "filter" ["all"
              ["==" "$type" "Point"]
              ["==" "meta" "vertex"]
              ["!=" "mode" "simple_select"]]
    "paint" {"circle-radius" ["case" ["==" ["get" "active"] "true"] 5 3]
             "circle-color" orange}}
   {"id" "gl-draw-midpoint"
    "type" "circle"
    "filter" ["all" ["==" "meta" "midpoint"]]
    "paint" {"circle-radius" 3
             "circle-color" orange}}])

;; ---------------------------------------------------------------------------
;; Props are communicated from the Reagent wrapper to the stable functional
;; component via a module-level atom.  This avoids creating a new React
;; component type on every render (which would cause useControl to remount).

(defonce ^:private draw-config (atom nil))

(defn- draw-component
  "Module-level React functional component with stable identity.
  Reads its configuration from `draw-config` — values from the first render
  are captured by useControl (which has [] hook deps)."
  []
  (let [{:keys [draw-props position on-create on-update on-delete on-render]} @draw-config
        ;; Captured during onCreate so the draw.render listener can pass the
        ;; MapboxDraw instance to consumers (for calling .getAll() etc.).
        draw-ref (volatile! nil)
        render-handler (fn [^js evt] (on-render evt @draw-ref))]
    (interop/use-control
     (fn []
       (let [d (MapboxDraw. draw-props)]
         (vreset! draw-ref d)
         d))
     (fn [^js ctx]
       (let [^js m (.-map ctx)]
         (.on m "draw.create" on-create)
         (.on m "draw.update" on-update)
         (.on m "draw.delete" on-delete)
         (.on m "draw.render" render-handler)))
     (fn [^js ctx]
       (let [^js m (.-map ctx)]
         (.off m "draw.create" on-create)
         (.off m "draw.update" on-update)
         (.off m "draw.delete" on-delete)
         (.off m "draw.render" render-handler)))
     #js {:position position})
    nil))

;; ---------------------------------------------------------------------------
;; Public

(defn draw-control
  "Reagent component wrapping @mapbox/mapbox-gl-draw via useControl.

  Props:
    :position   — control position string (optional)
    :on-create  — callback (fn [^js evt])  — evt.features is a JS array
    :on-update  — callback (fn [^js evt])  — evt.features, evt.action
    :on-delete  — callback (fn [^js evt])  — evt.features
    :on-render  — callback (fn [^js evt ^js draw]) — fires on every internal
                  store render (during drawing AND after mutations).  Call
                  (.getAll draw) to read the current FeatureCollection.

  All other props are passed to the MapboxDraw constructor (kebab-case keys
  converted to camelCase).

  Returns nil (the control renders nothing to the DOM)."
  [& args]
  (let [[prop-map _] (props/props-and-children args)
        prop-map     (or prop-map {})
        {:keys [position on-create on-update on-delete on-render]
         :or   {on-create (fn [])
                on-update (fn [])
                on-delete (fn [])
                on-render (fn [])}} prop-map
        rest-props   (dissoc prop-map :position :on-create :on-update :on-delete :on-render)
        styles       (get rest-props :styles default-styles)
        controls     (merge {:combine-features false :uncombine-features false}
                            (:controls rest-props))
        draw-props   (props/props->js (dissoc rest-props :styles :controls))]
    (aset draw-props "styles" (clj->js styles))
    (aset draw-props "controls"
          (let [obj (js-obj)]
            (doseq [[k v] controls]
              (aset obj (str/replace (name k) "-" "_") v))
            obj))
    (reset! draw-config
            {:draw-props draw-props
             :position   position
             :on-create  on-create
             :on-update  on-update
             :on-delete  on-delete
             :on-render  on-render})
    [:f> draw-component]))