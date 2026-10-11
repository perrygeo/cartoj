# Cartoj example

Self-contained ClojureScript project that consumes cartoj from your local
Maven cache and renders a single full-viewport map.

## Prerequisites

1. From the cartoj repo root, install the library to `~/.m2`:

   ```bash
   make install-local
   ```

   That produces `io.github.perrygeo/cartoj 0.1.0` in the local Maven cache.
   This `example/` directory pulls it in via `:mvn/version "0.1.0"` in
   `deps.edn`.

   Eventually, this will be on clojars.

2. Java 21+ and the `clojure` CLI must be on your PATH (shadow-cljs reads
   `deps.edn` via `:deps true`).

## Run

```bash
npm install
npm run dev
```

Open <http://localhost:8080>. You should see the MapLibre demo basemap
covering the viewport.

## What's in here

- `deps.edn` — single dep: `io.github.perrygeo/cartoj`. ClojureScript,
  Reagent, and re-frame come in transitively from cartoj's pom.
- `shadow-cljs.edn` — browser build, `:deps true` so dependencies are read
  from `deps.edn`.
- `package.json` — the minimum npm peers for any cartoj map: `react`,
  `react-dom`, `react-map-gl`, `maplibre-gl`. The `pmtiles`, geocoder, and
  draw npm packages are only needed by the optional `cartoj.pmtiles`,
  `cartoj.geocoder`, and `cartoj.draw` namespaces, which this example
  doesn't touch. Add `pmtiles` to your deps only if you require
  `cartoj.pmtiles` to register the `pmtiles://` protocol.
- `public/index.html` — loads `maplibre-gl.css` from unpkg and sizes the
  cartoj wrapper to fill the viewport.
- `src/example/core.cljs` — six lines: requires `cartoj.core`, mounts a
  Reagent root, renders `[cartoj/interactive-map …]`.

## Trying a newer cartoj version

After bumping `version` in the cartoj `build.clj`, re-run `make
install-local` from the repo root, then update `:mvn/version` here to match.
