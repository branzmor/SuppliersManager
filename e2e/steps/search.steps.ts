import { expect } from '@playwright/test';
import { Given, Then, When } from '../support/fixtures';

When('the user searches for potential suppliers for an order of {int} €', async ({ dashboard }, amount: number) => {
  await dashboard.searchFor(amount);
});

Given('the user has searched for potential suppliers for an order of {int} €', async ({ dashboard }, amount: number) => {
  await dashboard.searchFor(amount);
});

When('the user searches without entering an order amount', async ({ dashboard }) => {
  await dashboard.searchButton.click();
});

Then('the user is asked for an order amount', async ({ dashboard }) => {
  await expect(dashboard.amountInput).toBeVisible();
  await expect(dashboard.amountInput).toHaveValue('');
  await expect(dashboard.searchButton).toBeEnabled();
});

Then('the user is told {string}', async ({ dashboard }, message: string) => {
  await expect(dashboard.alert).toHaveText(message);
});

Then('the user is no longer shown an error', async ({ dashboard }) => {
  await expect(dashboard.alert).toBeHidden();
});

Then('no search is sent to the server', ({ searchEndpoint }) => {
  expect(searchEndpoint.requestCount).toBe(0);
});

Then('no supplier results are shown', async ({ dashboard }) => {
  await expect(dashboard.resultsTable).toBeHidden();
  await expect(dashboard.resultsSummary).toBeHidden();
});

Then('{int} suppliers are reported as found', async ({ dashboard }, total: number) => {
  await expect(dashboard.resultsSummary).toHaveText(`${total} suppliers found`);
});

Then('{int} of the {int} suppliers found is/are visible', async ({ dashboard }, visible: number, total: number) => {
  await expect(dashboard.resultsSummary).toHaveText(`${visible} visible suppliers out of ${total} total`);
});

Then('the user is told that no potential suppliers were found for this amount', async ({ dashboard }) => {
  await expect(dashboard.noResultsMessage).toBeVisible();
  await expect(dashboard.resultsTable).toBeHidden();
});
