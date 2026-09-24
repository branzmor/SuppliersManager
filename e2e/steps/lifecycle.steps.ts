import { expect } from '@playwright/test';
import { Given, Then, When } from '../support/fixtures';
import type { SupplierStatus, SustainabilityRating } from '../support/supplier-api';

Given('{string} from {string} has applied to become a supplier', async ({ scenario, supplierApi }, name: string, country: string) => {
  await supplierApi.registerCandidate(scenario.createSupplier(name, country));
});

Given(
  'the supervisor has accepted {string} with sustainability rating {string}',
  async ({ scenario, supplierApi }, name: string, rating: SustainabilityRating) => {
    await supplierApi.acceptCandidate(scenario.supplier(name).duns, rating);
  },
);

When(
  'the supervisor tries to accept {string} with sustainability rating {string}',
  async ({ scenario, supplierApi }, name: string, rating: SustainabilityRating) => {
    scenario.lastApiResponse = await supplierApi.tryAcceptCandidate(scenario.supplier(name).duns, rating);
  },
);

Given('the supervisor has refused {string}', async ({ scenario, supplierApi }, name: string) => {
  await supplierApi.refuseCandidate(scenario.supplier(name).duns);
});

Given('the supervisor has banned {string}', async ({ scenario, supplierApi }, name: string) => {
  await supplierApi.banSupplier(scenario.supplier(name).duns);
});

Then('the acceptance is rejected with {string}', async ({ scenario }, info: string) => {
  const response = scenario.lastApiResponse;
  expect(response?.status()).toBe(409);
  expect(await response?.json()).toEqual({ info });
});

When('the user looks for {string} among the potential suppliers', async ({ dashboard, scenario }, name: string) => {
  // The smallest order only this supplier (and those created alongside it) can take.
  await dashboard.searchFor(scenario.supplier(name).annualTurnover - 1);
  await dashboard.waitForSearchResult();
});

Then(
  '{string} is offered as a potential supplier with rating {string}',
  async ({ dashboard, scenario }, name: string, rating: string) => {
    const supplier = scenario.supplier(name);
    const row = dashboard.rowFor(supplier.duns);
    await expect(row).toHaveCount(1);
    await dashboard.expectRow(row, { Name: supplier.name, Country: supplier.country, Rating: rating });
  },
);

Then('{string} is not offered as a potential supplier', async ({ dashboard, scenario }, name: string) => {
  await expect(dashboard.rowFor(scenario.supplier(name).duns)).toHaveCount(0);
  // Not being on this page only proves anything if there is no further page to look at.
  if (await dashboard.nextPageButton.isVisible()) {
    await expect(dashboard.nextPageButton).toBeDisabled();
  }
});

Then('the supplier API reports {string} as {string}', async ({ scenario, supplierApi }, name: string, status: SupplierStatus) => {
  const supplier = await supplierApi.getSupplier(scenario.supplier(name).duns);
  expect(supplier.status).toBe(status);
});
