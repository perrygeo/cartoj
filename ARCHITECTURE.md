# Architecture

## Directory Layout

```
cartoj/
├── src/cartoj/          # Library source (6 public ns + 4 internal/support)
│   ├── core.cljs        # Main Map component, view-state converters
│   ├── overlays.cljs    # Marker, Popup wrappers
│   ├── sources.cljs     # Source, Layer wrappers (MapLibre style spec)
│   ├── controls.cljs    # NavigationControl, GeolocateControl, FullscreenControl,
│   │                      ScaleControl, AttributionControl, TerrainControl
│   ├── interop.cljs     # useMap / useControl hooks, with-map helper, MapProvider
│   ├── re_frame.cljs    # Optional re-frame bindings (event, sub, on-move helper)
│   ├── props.cljs       # Prop conversion engine (kebab-case CLJS → camelCase JS)
│   ├── draw.cljs        # Mapbox-GL-Draw integration via useControl
│   ├── geocoder.cljs    # Maplibre-geocoder integration (Nominatim)
│   └── pmtiles.cljs     # PMTiles protocol registration (side-effect only)
├── test/cartoj/         # One test file per source ns + demo tests
├── dev/cartoj/          # Demo harness (877-line interactive playground)
│   ├── demo.cljs        # 21-tab demo exercising every public API surface
│   └── demo_macros.clj  # Compile-time `code-string` macro for source display
├── public/              # Static assets: index.html, demo CSS, GeoJSON data
├── dist/                # :npm-module build output (gitignored)
├── target/              # Compilation artifacts (gitignored)
├── shadow-cljs.edn      # 3 build targets: :test, :dev, :lib
├── deps.edn             # CLJS, Reagent, re-frame
└── package.json         # react-map-gl, maplibre-gl, React 19
```

## Module Responsibilities

- **`cartoj.props`** — Central prop-conversion engine. Every other ns depends on it. Converts CLJS kebab-case keyword maps to camelCase JS objects, with special routing for nested maps: camelCase for JS/CSS props (`:style`, `:initial-view-state`), kebab-case preservation for MapLibre style spec props (`:paint`, `:layout`).
- **`cartoj.core`** — Reagent `interactive-map` component wrapping react-map-gl's `Map`. Handles `:class-name` via a wrapper div. Exports `view-state->js` and `view-state->clj` converters.
- **`cartoj.overlays`** — Simple wrappers: `marker` and `popup`. Required positional props: `:longitude`, `:latitude`.
- **`cartoj.sources`** — `source` and `layer` wrappers. Critical detail: `:paint` and `:layout` sub-maps preserve kebab-case keys (MapLibre style spec), not camelCase.
- **`cartoj.controls`** — Six control wrappers sharing a private `make-control` factory. All follow the same pattern: `props-and-children → props->js → [:r> Component js-props children]`.
- **`cartoj.interop`** — Hooks bridge. `use-map` and `use-control` are raw JS hook references (must be called inside `:f>`). `with-map` wraps `useMap` inside `:f>` so ordinary Reagent components can access the Map instance. `map-provider` wraps `MapProvider` for multi-map setups.
- **`cartoj.re_frame`** — Optional, additive. Auto-registers `::set-view-state` event and `::view-state` subscription on require. `on-move` helper returns a callback that dispatches view-state changes.
- **`cartoj.draw`** — MapboxDraw integration via `useControl`. Uses a module-level atom (`draw-config`) to bridge Reagent props into a stable-identity React functional component, avoiding remounts.
- **`cartoj.geocoder`** — Geocoder control using Nominatim forward-geocoding. Manages marker position via React `useState` inside a `:f>` functional component.
- **`cartoj.pmtiles`** — Side-effect-only ns. Registers `pmtiles://` protocol with maplibre-gl on load. Imported by `cartoj.core`.

## Dependency Graph

```
cartoj.props        ← (no deps beyond clojure.string)
cartoj.pmtiles      ← (maplibre-gl, pmtiles npm lib)

cartoj.core         ← cartoj.props, cartoj.pmtiles
cartoj.overlays     ← cartoj.props
cartoj.sources      ← cartoj.props
cartoj.controls     ← cartoj.props
cartoj.interop      ← cartoj.props

cartoj.re_frame     ← cartoj.core
cartoj.draw         ← cartoj.interop, cartoj.props
cartoj.geocoder     ← cartoj.interop, cartoj.overlays, cartoj.props
```

## Key Abstractions

### Prop Conversion Pipeline

Every component follows the same pattern:

```
CLJS args → props-and-children → [props-map, children]
                                ↓
                            props->js
                                ↓
              ┌─────────────────┴──────────────────┐
              │  per-value routing (convert-value)  │
              ├─────────────────┬──────────────────┤
              │ deep-convert    │ maplibre-spec    │ other
              │ keys (camel)    │ keys (kebab)     │ pass-through
              │ :style          │ :paint           │ :on-click
              │ :initial-view-  │ :layout          │ :id, :type
              │   state         │ :filter          │ scalars
              │ :bounds         │ :terrain         │
              │ :padding        │ :sky             │
              │ :fit-bounds-    │ :light           │
              │   options       │                  │
              └─────────────────┴──────────────────┘
                                ↓
                    JS object with camelCase keys
```

- `kebab->camel`: `:foo-bar` → `"fooBar"`; strings pass through unchanged.
- `map->js-camel`: recursive camelCase conversion for JS/CSS nested maps.
- `map->js-kebab`: recursive **kebab-case-preserving** conversion for MapLibre style spec maps.
- `convert-value`: routes each value to the right converter based on its key.
- Functions and scalars pass through as-is (verified via `identical?` in tests).

### Component Wrapping Patterns

Two patterns based on whether the underlying component uses hooks:

1. **Standard React components** (`:r>`): `[:r> ReactComponent js-props & children]`
   — Used by: interactive-map, marker, popup, source, layer, all 6 controls, map-provider.

2. **Hook-based components** (`:f>`): `[:f> functional-component]`
   — Used by: draw-control, geocoder-control, with-map.
   — Reason: Reagent wraps components as class components by default, so hooks can't be called directly. `:f>` creates a true React functional component where hooks work.

### View State Flow

```
Uncontrolled: Map reads :initial-view-state (JS) → manages internally
                                                      ↓
                                          onMove → JS viewState event
                                                      ↓
                                          view-state->clj → CLJS map
                                                      ↓
                                          (optional) re-frame dispatch

Controlled:   Reagent atom / re-frame sub → view-state->js → :view-state prop
                                                      ↓
                                          Map renders at given state
                                                      ↓
                                          onMove → callback updates atom/sub
```

## Design Decisions

- **Maplibre-only.** Only `react-map-gl/maplibre` imports. No Mapbox GL JS dependency. Switch to conditional imports if Mapbox support is needed later.
- **re-frame is additive.** `cartoj.re-frame` is a separate namespace not required by any other cartoj ns. The core library works with plain Reagent atoms.
- **Prop conversion is shallow with known-key recursion.** Top-level keys are converted. Nested maps are only recursed for a fixed set of known keys (`deep-convert-keys` set). This avoids unnecessary traversal and prevents converting user data that happens to be a map.
- **MapLibre style spec keys stay kebab-case.** `:paint`, `:layout`, `:filter`, `:terrain`, `:sky`, `:light` sub-maps use `map->js-kebab` because MapLibre GL JS expects `"circle-radius"` not `"circleRadius"`. This is the most subtle part of the conversion engine.
- **Events are untouched JS objects.** Callbacks receive the native JS event from react-map-gl — no automatic CLJS conversion. Consumers call `.-type`, `.-lngLat`, etc. or use `js->clj`.
- **`:class-name` on interactive-map uses a wrapper div.** The underlying react-map-gl `Map` only forwards `:id`, `:ref`, `:style` to its container element. Cartoj wraps in a `div.cartoj-interactive-map` (class overridable) to support `:class-name`.
- **Atom bridge for hook-based controls.** `cartoj.draw` uses a module-level `draw-config` atom to pass changing props into a stable-identity React functional component. This prevents remounts that would destroy the Draw instance.
- **Testing via hiccup shape verification.** Tests call component functions and inspect the returned hiccup vectors. No DOM/WebGL mounting. This mirrors patterns from reagent-material-ui and headlessui-reagent.

## External Dependencies

### Clojure/CLJS (deps.edn)
- `org.clojure/clojurescript` 1.12+ — compiler
- `reagent` 2.0+ — React wrapper, `:r>` / `:f>` adaptors
- `re-frame` 1.4+ — optional state management (consumer brings their own)

### JavaScript (package.json)
- `react-map-gl/maplibre` ^8.0 — `Map`, `Marker`, `Popup`, `Source`, `Layer`, all controls, `useMap`, `useControl`, `MapProvider`
- `maplibre-gl` ^5.0 — underlying WebGL map engine, protocol registration
- `react` / `react-dom` ^19.0 — peer deps of react-map-gl
- `@maplibre/maplibre-gl-geocoder` ^1.7 — geocoder (only if using `cartoj.geocoder`)
- `pmtiles` ^4.4 — PMTiles protocol (only if using `cartoj.pmtiles`)
- `@mapbox/mapbox-gl-draw` ^1.4 — drawing tools (only if using `cartoj.draw`)

### Build
- `shadow-cljs` ^2.28 — all compilation, bundling, dev server, and npm-module output.

## Entry Points

| Mode | Command | Entry | Output |
|------|---------|-------|--------|
| **Dev** | `npm run dev` | `cartoj.demo/init` | Browser on port 3000, `public/js/main.js` |
| **Test** | `npm test` | auto-discovers `*-test$` ns | Node.js, `target/test/test.js` |
| **Lib** | `npm run build` | 9 public ns as npm-module entries | `dist/` directory |

The `:npm-module` build exports: `cartoj.core`, `cartoj.overlays`, `cartoj.sources`, `cartoj.controls`, `cartoj.interop`, `cartoj.re-frame`, `cartoj.draw`, `cartoj.geocoder`, `cartoj.pmtiles`.

## Namespaces

- `cartoj.core` — `interactive-map`, `view-state->js`, `view-state->clj`
- `cartoj.overlays` — `marker`, `popup`
- `cartoj.sources` — `source`, `layer`
- `cartoj.controls` — `navigation-control`, `geolocate-control`, `fullscreen-control`, `scale-control`, `attribution-control`, `terrain-control`
- `cartoj.geocoder` — `geocoder-control` _(needs `@maplibre/maplibre-gl-geocoder`)
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
