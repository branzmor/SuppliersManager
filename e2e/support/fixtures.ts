import { test as base, createBdd } from 'playwright-bdd';
import { DashboardPage } from '../pages/DashboardPage';
import { API_URL } from './env';
import { ScenarioState } from './scenario-state';
import { SearchEndpoint } from './search-endpoint';
import { SupplierApi } from './supplier-api';

interface Fixtures {
  dashboard: DashboardPage;
  searchEndpoint: SearchEndpoint;
  supplierApi: SupplierApi;
  scenario: ScenarioState;
}

export const test = base.extend<Fixtures>({
  dashboard: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },
  // Auto: request recording must start before the first user action, not when a step first
  // asks about it, or "no search is sent" would pass vacuously.
  searchEndpoint: [
    async ({ page }, use) => {
      await use(new SearchEndpoint(page));
    },
    { auto: true },
  ],
  supplierApi: async ({ playwright }, use) => {
    const context = await playwright.request.newContext({ baseURL: API_URL });
    await use(new SupplierApi(context));
    await context.dispose();
  },
  scenario: async ({}, use) => {
    await use(new ScenarioState());
  },
});

export const { Given, When, Then } = createBdd(test);
