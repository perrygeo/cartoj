# Cartoj

A ClojureScript / Reagent wrapper around [react-map-gl](https://visgl.github.io/react-map-gl/) (MapLibre edition).

## Status

**Alpha** — API is stable but not yet published to Clojars.

## Requirements

- React 19 + Reagent 2.0
- MapLibre GL JS ≥ 5.0 (open-source, no token required)
- shadow-cljs

## Installation

Add to `deps.edn` (or `shadow-cljs.edn` `:dependencies`):

```clojure
;; coming soon — not yet on Clojars
io.github.your-org/cartoj {:git/sha "..."}
```

Install npm dependencies:

```bash
npm install react react-dom react-map-gl maplibre-gl
```

## Usage

### Uncontrolled map

```clojure
(require '[cartoj.core     :as map]
         '[cartoj.controls :as ctrl]
         '[cartoj.overlays :as overlay])

[map/map
 {:initial-view-state {:longitude -122.4 :latitude 37.8 :zoom 14}
  :map-style "https://demotiles.maplibre.org/style.json"
  :style {:width "100%" :height "400px"}
  :on-click (fn [e] (println (.-lngLat e)))}
 [ctrl/navigation-control {:position "top-right"}]
 [overlay/marker {:longitude -122.4 :latitude 37.8}]]
```

### Controlled map (plain Reagent)

```clojure
(defonce view-state
  (r/atom {:longitude -122.4 :latitude 37.8 :zoom 14}))

[map/map
 (merge @view-state
        {:map-style "https://demotiles.maplibre.org/style.json"
         :style {:width "100%" :height "400px"}
         :on-move #(reset! view-state (core/view-state->clj (.-viewState %)))})]
```

### Controlled map (re-frame)

```clojure
(require '[cartoj.core     :as map]
         '[cartoj.re-frame :as cartoj-rf]
         '[re-frame.core   :as rf])

;; In your view:
(let [vs (or @(rf/subscribe [::cartoj-rf/view-state])
             {:longitude -122.4 :latitude 37.8 :zoom 14})]
  [map/map
   (merge vs {:map-style "https://demotiles.maplibre.org/style.json"
              :on-move   (cartoj-rf/on-move)})])
```

### GeoJSON Source + Layer

```clojure
(require '[cartoj.sources :as sources])

[map/map {:initial-view-state {...} :map-style "..."}
 [sources/source {:id "pts" :type "geojson" :data my-geojson-js-object}
  [sources/layer {:id    "pts-layer"
                  :type  "circle"
                  :source "pts"
                  :paint {:circle-radius 6
                          :circle-color  "#e00"}}]]]
```

### Marker + Popup

```clojure
(require '[cartoj.overlays :as overlay])

[map/map {...}
 [overlay/marker {:longitude -122.4 :latitude 37.8
                  :on-click #(reset! show? true)}]
 (when @show?
   [overlay/popup {:longitude -122.4 :latitude 37.8
                   :on-close  #(reset! show? false)}
    [:p "Hello!"]])]
```

### Imperative access via `useMap`

```clojure
(require '[cartoj.interop :as interop])

[interop/with-map
 (fn [map-ref]
   (when map-ref
     (.flyTo map-ref (clj->js {:center [-122.4 37.8] :zoom 16})))
   nil)]
```

## Namespaces

- `cartoj.core` — `map` component, `view-state->js`, `view-state->clj`
- `cartoj.overlays` — `marker`, `popup`
- `cartoj.sources` — `source`, `layer`
- `cartoj.controls` — `navigation-control`, `geolocate-control`, `fullscreen-control`, `scale-control`, `attribution-control`
- `cartoj.interop` — `with-map`, `map-provider`, `use-map`, `use-control`
- `cartoj.re-frame` — `::set-view-state`, `::view-state`, `on-move` _(optional, requires re-frame)_

## Prop conventions

- All props use **kebab-case keywords** (`:initial-view-state`, `:on-click`, `:map-style`)
- Cartoj converts them to camelCase JS before passing to react-map-gl
- Callback props receive the **native JS event object** (no automatic conversion)
- Nested data props (`:initial-view-state`, `:style`, `:paint`, `:layout`, `:filter`) are converted to JS objects via `clj->js`

## Development

```bash
# Install deps
npm install

# Run tests (requires Java 21+)
npm test

# Start dev server
npm run dev
# Then open http://localhost:3000
```

## License

MIT
