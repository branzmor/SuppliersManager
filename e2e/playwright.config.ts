import { defineConfig, devices } from '@playwright/test';
import { defineBddProject } from 'playwright-bdd';
import { FRONTEND_URL } from './support/env';

const CI = Boolean(process.env.CI);

const bdd = {
  features: 'features/**/*.feature',
  steps: ['steps/**/*.ts', 'support/fixtures.ts'],
};

export default defineConfig({
  // Builds, (re)creates and seeds the dockerised stack; see support/stack.ts for why this isn't `webServer`.
  globalSetup: './support/global-setup.ts',
  globalTeardown: './support/global-teardown.ts',
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: true,
  forbidOnly: CI,
  retries: CI ? 2 : 0,
  workers: CI ? 2 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    ...devices['Desktop Chrome'],
    baseURL: FRONTEND_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    // Read-only scenarios asserting the exact reference catalogue, plus the @network ones.
    defineBddProject({ name: 'dashboard', ...bdd, tags: 'not @lifecycle' }),
    {
      // These create suppliers that every later search would also list, so they only start once
      // the scenarios that depend on the untouched catalogue are done.
      ...defineBddProject({ name: 'lifecycle', ...bdd, tags: '@lifecycle' }),
      dependencies: ['dashboard'],
    },
  ],
});
