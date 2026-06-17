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


The demo at `dev/cartoj/demo.cljs` exercises every namespace. To run it locally you need the full set of npm dependencies.

```bash
npm install  # pinned in package.json
```

Alternatively:

```bash
npm install react react-dom \
  react-map-gl maplibre-gl \
  @maplibre/maplibre-gl-geocoder \
  @mapbox/mapbox-gl-draw \
  pmtiles
```
