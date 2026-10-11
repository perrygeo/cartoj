import { test, expect } from '@playwright/test';
import { openTab, waitForMapsIdle, expectHealthyMap, cartojState } from './helpers';
import { TABS } from './tabs';

/**
 * Full integration sweep across every demo tab.
 *
 * For each tab we:
 *   1. Navigate to it by URL hash and wait for the instrumentation hook.
 *   2. Wait for all its maps to load + reach idle.
 *   3. Assert the canvas(es) render and no map emitted a load error.
 *
 * Visual-regression screenshots are added as a separate, opt-in suite so the
 * DOM/behavior checks stay fast and robust across environments.
 */
test.describe('demo tabs', () => {
  for (const tab of TABS) {
    test(`${tab.id} renders ${tab.maps} map(s)`, async ({ page }) => {
      const consoleErrors: string[] = [];
      page.on('pageerror', (err) => consoleErrors.push(String(err)));

      await openTab(page, tab.id);
      await waitForMapsIdle(page, tab.maps);
      await expectHealthyMap(page, tab.maps);

      // No uncaught JS exceptions while mounting/rendering the tab.
      expect(consoleErrors, `uncaught errors on #${tab.id}`).toEqual([]);
    });
  }
});

/**
 * Tab-navigation behavior: clicking the sidebar links (not just deep-linking by
 * hash) swaps the active tab and remounts the map. Exercises the in-app
 * reset-on-tab-change path of the instrumentation.
 */
test.describe('tab navigation', () => {
  test('clicking sidebar links switches tabs and remounts maps', async ({ page }) => {
    await openTab(page, 'barebones');
    await waitForMapsIdle(page, 1);

    // Click through to the controls tab via the sidebar. The nav items are
    // <a> elements without href (click handlers), so target by class + text.
    await page.locator('.nav-item', { hasText: 'Controls' }).click();
    await page.waitForFunction(() => window.location.hash === '#controls');
    await waitForMapsIdle(page, 1);
    await expectHealthyMap(page, 1);

    // Counters should reflect only the current tab (reset on change).
    const state = await cartojState(page);
    expect(state.loadCount).toBeGreaterThanOrEqual(1);
  });
});

/**
 * Opt-in visual regression. Run with:
 *   UPDATE_SNAPSHOTS=1 npx playwright test --config playwright.config.ts --grep @visual
 * to create baselines, then without the env var to compare. Baselines are
 * environment-sensitive (GPU/fonts); pin them to one canonical runner (e.g. CI).
 */
test.describe('@visual', () => {
  test.skip(!process.env.CARTOJ_VISUAL, 'set CARTOJ_VISUAL=1 to run visual tests');

  for (const tab of TABS) {
    test(`${tab.id} canvas matches golden`, async ({ page }) => {
      await openTab(page, tab.id);
      await waitForMapsIdle(page, tab.maps);
      // Screenshot the map container(s). Mask dynamic UI chrome if needed.
      const container = page.locator('.cartoj-interactive-map, .half-interactive-map').first();
      await expect(container).toHaveScreenshot(`${tab.id}.png`);
    });
  }
});
