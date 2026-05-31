# Cartoj

A ClojureScript / Reagent wrapper around [react-map-gl](https://visgl.github.io/react-map-gl/) (MapLibre edition).

## Status

**Alpha** — API is stable but not yet published to Clojars.

## Requirements

- React 19 + Reagent 2.0
- MapLibre GL JS ≥ 5.0 (open-source, no token required)
- shadow-cljs

## Installation

TODO

Add to `deps.edn` (or `shadow-cljs.edn` `:dependencies`):

```clojure
;; coming soon — not yet on Clojars
io.github.your-org/cartoj {:git/sha "..."}
```

Install npm dependencies:

```bash
npm install react react-dom react-map-gl maplibre-gl
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


## License

MIT
