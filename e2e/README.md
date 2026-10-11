# Cartoj end-to-end tests

Playwright integration tests that drive the demo harness (`dev/cartoj/demo.cljs`)
in a real browser, exercising every example tab.

## Why Playwright

The MapLibre / react-map-gl ecosystem (react-map-gl, mapbox-gl-js, deck.gl,
OpenLayers) standardizes on **Playwright** for in-browser render/integration
tests, including cross-engine coverage (Chromium / Firefox / **WebKit** = Safari
engine on CI) and canvas visual regression via screenshots. These tests follow
the same patterns but target the full demo app rather than individual components.

## How "map ready" works

MapLibre renders asynchronously to a WebGL canvas, so tests must wait for a
deterministic signal rather than sleeping. The demo installs dev-only
instrumentation (`dev/cartoj/test_hooks.cljs`) that wraps
`cartoj.core/interactive-map` and exposes a `window.__cartoj` object:

| field        | meaning                                            |
|--------------|----------------------------------------------------|
| `maps`       | live MapLibre map instances for the current tab    |
| `loadCount`  | maps that have fired `load`                         |
| `idleCount`  | maps that have reached `idle` (tiles + anim settled)|
| `errorCount` | maps that emitted an `error`                        |
| `reset()`    | clears counters (auto-called on tab change)         |

`waitForMapsIdle()` (in `tests/helpers.ts`) polls these to know when a tab's
maps have finished rendering. This mirrors react-map-gl's render harness, which
waits on `map.once('idle')`.

The instrumentation is **not** part of the published library — it only loads via
the demo and leaves the on-screen code samples untouched (it patches the var in
place at runtime instead of editing each section).

## Running

```bash
# from repo root (starts/reuses `shadow-cljs watch dev` on :3000 automatically)
npm run test:e2e

# interactive UI mode
npm run test:e2e:ui

# just the smoke subset
npx playwright test --config e2e/playwright.config.ts smoke.spec.ts
```

### Browser binary

On this Nix host the Playwright-bundled Chromium can't resolve system libraries,
so the config points at the system Chromium. Override the path if needed:

```bash
CARTOJ_CHROMIUM=/path/to/chromium npm run test:e2e
```

On platforms where the bundled browsers work, install them with
`npx playwright install chromium` and delete the `executablePath` override in
`playwright.config.ts`.

## Cross-browser

Firefox and WebKit projects are scaffolded (commented) in
`playwright.config.ts`. Enable them after installing the engines
(`npx playwright install firefox webkit`) and validating headless WebGL renders
cleanly for the map canvas.

## Visual regression (opt-in)

DOM/behavior assertions run by default. Canvas screenshot comparison is gated
behind `CARTOJ_VISUAL` because baselines are GPU/font-sensitive and should be
pinned to one canonical environment (e.g. CI / a container):

```bash
# create/update golden images
npm run test:e2e:update

# compare against goldens
CARTOJ_VISUAL=1 npx playwright test --config e2e/playwright.config.ts --grep @visual
```

## Layout

```
e2e/
├── playwright.config.ts   # projects, webServer, timeouts, thresholds
├── tests/
│   ├── helpers.ts         # openTab / waitForMapsIdle / expectHealthyMap
│   ├── tabs.ts            # tab ids + expected map counts (mirror demo.cljs)
│   ├── smoke.spec.ts      # 3-tab proof of concept
│   └── demo.spec.ts       # full sweep over all tabs + nav + @visual suite
```
