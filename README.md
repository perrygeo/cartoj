# Cartoj

[![Test](https://github.com/perrygeo/cartoj/actions/workflows/test.yml/badge.svg)](https://github.com/perrygeo/cartoj/actions/workflows/test.yml)

A ClojureScript / Reagent wrapper around [react-map-gl](https://visgl.github.io/react-map-gl/) (MapLibre edition).

## Status

**Alpha** — API is stable, not yet published to Clojars.

## Requirements

- React 19 + Reagent 2.0
- MapLibre GL JS ≥ 5.0 (open-source, no token required)
- shadow-cljs (or any cljs build that understands `deps.cljs`)

## Installation

> Not yet on Clojars. The deps coordinate below is the target shape — see [Publishing](#publishing) for the release pipeline.

Add to `deps.edn`:

```clojure
{:deps {io.github.perrygeo/cartoj {:mvn/version "0.1.0"}}}
```

Or in `shadow-cljs.edn`:

```clojure
:dependencies [[io.github.perrygeo/cartoj "0.1.0"]]
```

## npm dependencies

Cartoj ships a `deps.cljs` so shadow-cljs will warn if any of the following are missing from your `package.json`. To use cartoj **at all**, install:

```bash
npm install react react-dom react-map-gl maplibre-gl pmtiles
```

`pmtiles` is required because `cartoj.core` loads `cartoj.pmtiles` to register the `pmtiles://` protocol with MapLibre. The remaining packages are only needed by specific namespaces — install whichever ones you use:

- `@maplibre/maplibre-gl-geocoder` — required by `cartoj.geocoder`
- `@mapbox/mapbox-gl-draw` — required by `cartoj.draw`

### Running the bundled demo

The demo at `dev/cartoj/demo.cljs` exercises every namespace. To run it locally you need the full set:

```bash
npm install react react-dom \
  react-map-gl maplibre-gl \
  @maplibre/maplibre-gl-geocoder \
  @mapbox/mapbox-gl-draw \
  pmtiles
```

These are already pinned in `package.json` — `make install` will pull them all.

The demo's HTML page (`public/index.html`) also loads several CSS files and one JS file from CDN. They are **not npm deps** of cartoj; they are demo-page chrome:

- `https://unpkg.com/bamboo.css` — page styling
- `https://unpkg.com/maplibre-gl/dist/maplibre-gl.css` — required for any cartoj map to render correctly
- `https://unpkg.com/@maplibre/maplibre-gl-geocoder/dist/maplibre-gl-geocoder.css` — geocoder widget styling
- `https://unpkg.com/@mapbox/mapbox-gl-draw@1.4.3/dist/mapbox-gl-draw.css` — draw control styling
- `https://unpkg.com/@highlightjs/cdn-assets@11.9.0/...` — `highlight.js` + Clojure language pack for the code-block sidebar

When you embed cartoj in your own app, you must include `maplibre-gl.css` (and the geocoder/draw CSS if you use those namespaces). The other CDN assets are demo-only.

## Namespaces

- `cartoj.core` — `interactive-map`, `view-state->js`, `view-state->clj`
- `cartoj.overlays` — `marker`, `popup`
- `cartoj.sources` — `source`, `layer`
- `cartoj.controls` — `navigation-control`, `geolocate-control`, `fullscreen-control`, `scale-control`, `attribution-control`, `terrain-control`
- `cartoj.geocoder` — `geocoder-control` _(needs `@maplibre/maplibre-gl-geocoder`)_
- `cartoj.draw` — `draw-control` _(needs `@mapbox/mapbox-gl-draw`)_
- `cartoj.pmtiles` — PMTiles protocol registration _(needs `pmtiles`)_
- `cartoj.interop` — `with-map`, `map-provider`, `use-map`, `use-control`
- `cartoj.re-frame` — `::set-view-state`, `::view-state`, `on-move` _(optional, requires re-frame)_

## Prop conventions

- All props use **kebab-case keywords** (`:initial-view-state`, `:on-click`, `:map-style`)
- Cartoj converts them to camelCase JS before passing to react-map-gl
- Callback props receive the **native JS event object** (no automatic conversion)
- Nested data props (`:initial-view-state`, `:style`, `:paint`, `:layout`, `:filter`) are converted recursively with key camelCasing

## Publishing

Cartoj is distributed as a **source-only JAR on Clojars**. Consumers' own ClojureScript compiler (typically shadow-cljs) compiles the `.cljs` files alongside their app code, so Google Closure `:advanced` optimization works across the whole bundle.

The release pipeline is `build.clj` (using `tools.build` and `slipset/deps-deploy`):

```bash
make jar             # build target/cartoj-<version>.jar
make install-local   # install to ~/.m2 for testing from another local project
make deploy          # push to Clojars (needs CLOJARS_USERNAME / CLOJARS_PASSWORD)
```

Bump `version` in `build.clj` for each release and tag the commit `v<version>`.

The `make build` target (shadow-cljs `:lib` → `dist/`) produces an **npm-module** bundle for JS/TS consumers. That is a separate distribution channel from Clojars and is currently unpublished.

## License

MIT
