import { existsSync } from 'node:fs';
import { defineConfig, devices } from '@playwright/test';

/**
 * On this (Nix) host the Playwright-bundled Chromium can't find system
 * libraries, so we use the system Chromium instead. Set CARTOJ_CHROMIUM to
 * point elsewhere. When unset we use the Nix path if it exists, otherwise we
 * fall back to Playwright's bundled Chromium (used on CI, where it is
 * installed with `npx playwright install --with-deps chromium`).
 */
const NIX_CHROMIUM = '/etc/profiles/per-user/mperry/bin/chromium';

function chromiumPath(): string | undefined {
  if (process.env.CARTOJ_CHROMIUM) return process.env.CARTOJ_CHROMIUM;
  if (existsSync(NIX_CHROMIUM)) return NIX_CHROMIUM;
  return undefined; // Playwright-bundled Chromium
}

/**
 * Playwright end-to-end tests for the cartoj demo harness.
 *
 * The demo is a single page (public/index.html) whose 21+ example tabs are
 * selected via the URL hash (e.g. #controls). Each tab mounts one or more
 * MapLibre maps. Tests wait on the `window.__cartoj` instrumentation installed
 * by cartoj.test-hooks (see dev/cartoj/test_hooks.cljs) to know when maps have
 * finished rendering, then assert on the DOM and (optionally) compare canvas
 * screenshots against golden images.
 *
 * The demo is served by `shadow-cljs watch dev` on port 3000. If it is already
 * running it is reused; otherwise Playwright starts it.
 */
export default defineConfig({
  testDir: './tests',
  // Keep artifacts under e2e/ (resolves relative to this config file).
  // Without this, Playwright writes to <cwd>/test-results, and we run from the repo root.
  outputDir: './test-results',
  // MapLibre + remote tiles can be slow; give each test room.
  timeout: 60_000,
  expect: {
    timeout: 15_000,
    // Loose pixel threshold: WebGL/GPU/font differences across environments
    // make exact matches brittle. Tightened per-assertion where it matters.
    toHaveScreenshot: { maxDiffPixelRatio: 0.05 },
  },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: { executablePath: chromiumPath() },
      },
    },
    // Cross-browser projects — enable once baselines are stable and the
    // corresponding engines are installed (`npx playwright install firefox webkit`).
    // { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    // { name: 'webkit',  use: { ...devices['Desktop Safari'] } },
  ],
  webServer: {
    command: 'npx shadow-cljs watch dev',
    url: 'http://localhost:3000/index.html',
    reuseExistingServer: true,
    timeout: 120_000,
    cwd: '..',
  },
});
