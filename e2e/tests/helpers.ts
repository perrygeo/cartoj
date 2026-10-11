import { Page, expect } from '@playwright/test';

/**
 * Shape of the instrumentation object installed on `window` by
 * cartoj.test-hooks. Mirrors dev/cartoj/test_hooks.cljs.
 */
export interface CartojState {
  maps: unknown[];
  loadCount: number;
  idleCount: number;
  errorCount: number;
}

declare global {
  interface Window {
    __cartoj?: CartojState;
  }
}

/**
 * Open a demo tab by its URL hash id (e.g. "controls") on a freshly loaded
 * page, and wait until the instrumentation hook is present.
 */
export async function openTab(page: Page, tabId: string): Promise<void> {
  await page.goto(`/index.html#${tabId}`);
  // Wait for the app to mount and the test-hooks instrumentation to install.
  await page.waitForFunction(() => Boolean(window.__cartoj), undefined, {
    timeout: 20_000,
  });
}

/**
 * Wait until `expected` maps have loaded and all loaded maps have reached the
 * MapLibre `idle` state (tiles fetched, animations settled). This is the
 * deterministic "map is ready" signal recommended by the react-map-gl render
 * test harness, surfaced to us via window.__cartoj.
 */
export async function waitForMapsIdle(page: Page, expected = 1): Promise<void> {
  // First, wait for the expected number of maps to fire `load`.
  await page.waitForFunction(
    (n) => (window.__cartoj?.loadCount ?? 0) >= n,
    expected,
    { timeout: 40_000 },
  );
  // Then wait for all loaded maps to become idle.
  await page.waitForFunction(
    (n) => {
      const s = window.__cartoj;
      return Boolean(s) && s!.idleCount >= n && s!.idleCount >= s!.loadCount;
    },
    expected,
    { timeout: 40_000 },
  );
  // Small settle to allow any final paint/animation to flush, mirroring the
  // 500ms settle in react-map-gl's render harness.
  await page.waitForTimeout(300);
}

/** Read the current instrumentation snapshot. */
export async function cartojState(page: Page): Promise<CartojState> {
  return page.evaluate(() => window.__cartoj as CartojState);
}

/**
 * Assert the baseline health of a tab: the canvas is visible and no map
 * emitted an error during load.
 */
export async function expectHealthyMap(page: Page, expected = 1): Promise<void> {
  const canvases = page.locator('.maplibregl-canvas');
  await expect(canvases).toHaveCount(expected);
  await expect(canvases.first()).toBeVisible();
  const state = await cartojState(page);
  expect(state.errorCount, 'map should not emit load errors').toBe(0);
}
