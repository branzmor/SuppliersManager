import { expect } from '@playwright/test';
import type { DataTable } from 'playwright-bdd';
import type { SortOrder } from '../pages/DashboardPage';
import { Then, When } from '../support/fixtures';

Then('the results show these suppliers in this order:', async ({ dashboard }, table: DataTable) => {
  await dashboard.expectRows(table.hashes());
});

Then('{int} suppliers are shown', async ({ dashboard }, count: number) => {
  await expect(dashboard.dataRows).toHaveCount(count);
});

Then('the first supplier shown is {string}', async ({ dashboard }, name: string) => {
  await dashboard.expectRow(dashboard.dataRows.first(), { Name: name });
});

Then('the last supplier shown is {string}', async ({ dashboard }, name: string) => {
  await dashboard.expectRow(dashboard.dataRows.last(), { Name: name });
});

When('the user sorts the results by {string}', async ({ dashboard }, column: string) => {
  await dashboard.sortBy(column);
});

Then(
  /^the results are sorted by "([^"]+)" in (ascending|descending) order$/,
  async ({ dashboard }, column: string, order: SortOrder) => {
    await dashboard.expectSortedBy(column, order);
  },
);

Then('the results are no longer sorted by {string}', async ({ dashboard }, column: string) => {
  await expect(dashboard.columnHeader(column)).toHaveAttribute('aria-sort', 'none');
});

When(/^the user goes to the (next|previous) page$/, async ({ dashboard }, direction: string) => {
  await (direction === 'next' ? dashboard.nextPageButton : dashboard.previousPageButton).click();
});

Then(/^the user cannot go to the (next|previous) page$/, async ({ dashboard }, direction: string) => {
  await expect(direction === 'next' ? dashboard.nextPageButton : dashboard.previousPageButton).toBeDisabled();
});

Then('the user is on page {int} of {int}', async ({ dashboard }, page: number, pages: number) => {
  await expect(dashboard.pageIndicator).toHaveText(`Page ${page} of ${pages}`);
});
