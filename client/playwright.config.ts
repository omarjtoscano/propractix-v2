import { defineConfig, devices } from '@playwright/test';

if (!process.env.PROPRACTIX_E2E_BASE_URL) throw new Error('Run npm run client:e2e');

export default defineConfig({
  testDir: './e2e',
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
