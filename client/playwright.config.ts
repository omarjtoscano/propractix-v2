import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  globalSetup: './e2e/global-setup.ts',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  forbidOnly: true,
  timeout: 120_000,
  expect: { timeout: 10_000 },
  globalTimeout: 600_000,
  reporter: [['list'], ['json', { outputFile: '../.local/e2e/results.json' }]],
  outputDir: '../.local/e2e/test-results',
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  use: {
    baseURL: process.env.PROPRACTIX_E2E_BASE_URL,
    // Browser traces retain query/cursor and manual values. Do not collect them.
    trace: 'off',
    screenshot: 'off',
    video: 'off',
  },
});
