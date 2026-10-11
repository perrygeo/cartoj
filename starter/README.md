# Cartoj starter

Self-contained ClojureScript project that consumes cartoj and renders a
single full-viewport map.

## Prerequisites

1. Install cartoj to your local Maven cache from the repo root:

   ```bash
   make install-local
   ```

   That produces `io.github.perrygeo/cartoj 0.4.0-alpha1` in `~/.m2`. This
   `starter/` directory pulls it in via `:mvn/version "0.4.0-alpha1"` in
   `deps.edn`. The same version is published on Clojars, so you can also
   point a fresh project at Clojars instead of the local cache.

2. Java 21+ and the `clojure` CLI must be on your PATH (shadow-cljs reads
   `deps.edn` via `:deps`).

## Run

```bash
npm install
npm run dev
```

Open <http://localhost:8080>. You should see the MapLibre demo basemap
covering the viewport.

## What's in here

- `deps.edn` — single dep: `io.github.perrygeo/cartoj`. ClojureScript and
  Reagent come in transitively from cartoj's pom. (re-frame is not a cartoj
  dependency — add it yourself if you use re-frame.)
- `shadow-cljs.edn` — browser build, `:deps {:aliases [:shadow-cljs]}` so
  dependencies are read from `deps.edn`.
- `package.json` — the minimum npm peers for any cartoj map: `react`,
  `react-dom`, `react-map-gl`, `maplibre-gl`. The `pmtiles`, geocoder, and
  draw npm packages are only needed by the optional `cartoj.pmtiles`,
  `cartoj.geocoder`, and `cartoj.draw` namespaces, which this example
  doesn't touch. Add `pmtiles` to your deps only if you require
  `cartoj.pmtiles` to register the `pmtiles://` protocol.
- `public/index.html` — loads `maplibre-gl.css` from unpkg and sizes the
  cartoj wrapper to fill the viewport.
- `src/example/core.cljs` — a few lines: requires `cartoj.core`, mounts a
  Reagent root, renders `[cartoj/interactive-map …]`.

## Trying a newer cartoj version

After bumping `VERSION` in the cartoj repo, re-run `make install-local` from
the repo root, then update `:mvn/version` here to match (and update the
root `scripts/check-version.js` list if it references this file).
