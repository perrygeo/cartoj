import { test, expect } from '@playwright/test';
import { openTab, waitForMapsIdle, expectHealthyMap } from './helpers';

/**
 * Proof-of-concept smoke test exercising a few representative tabs:
 *   - barebones:  the minimal map
 *   - controls:   map with navigation/geolocate/etc. controls
 *   - sources:    GeoJSON source + circle layer
 *
 * Verifies the instrumentation + readiness flow end-to-end before we expand
 * to every tab in demo.spec.ts.
 */
test.describe('smoke', () => {
  test('barebones map renders', async ({ page }) => {
    await openTab(page, 'barebones');
    await waitForMapsIdle(page, 1);
    await expectHealthyMap(page, 1);
  });

  test('controls tab renders with control widgets', async ({ page }) => {
    await openTab(page, 'controls');
    await waitForMapsIdle(page, 1);
    await expectHealthyMap(page, 1);
    // react-map-gl renders controls into .maplibregl-ctrl containers.
    await expect(page.locator('.maplibregl-ctrl').first()).toBeVisible();
  });

  test('sources tab renders GeoJSON layer', async ({ page }) => {
    await openTab(page, 'sources');
    await waitForMapsIdle(page, 1);
    await expectHealthyMap(page, 1);
  });
});
