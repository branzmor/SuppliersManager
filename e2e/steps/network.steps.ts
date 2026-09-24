import { expect } from '@playwright/test';
import { Given, Then, When } from '../support/fixtures';

Given('the server is slow to answer supplier searches', async ({ searchEndpoint }) => {
  await searchEndpoint.holdResponses();
});

When('the server answers the search', ({ searchEndpoint }) => {
  searchEndpoint.releaseResponses();
});

Given(
  'the supplier search fails with status {int} and the message {string}',
  async ({ searchEndpoint }, status: number, message: string) => {
    await searchEndpoint.failWith(status, message);
  },
);

Given('the supplier search fails with status {int} and no message', async ({ searchEndpoint }, status: number) => {
  await searchEndpoint.failWith(status);
});

Given('the server cannot be reached', async ({ searchEndpoint }) => {
  await searchEndpoint.failConnection();
});

When('the server becomes reachable again', async ({ searchEndpoint }) => {
  await searchEndpoint.restore();
});

Then('the user sees that suppliers are loading', async ({ dashboard }) => {
  await expect(dashboard.loadingIndicator).toBeVisible();
});

Then('the user no longer sees that suppliers are loading', async ({ dashboard }) => {
  await expect(dashboard.loadingIndicator).toBeHidden();
});
