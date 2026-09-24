import { expect } from '@playwright/test';
import type { DashboardPage } from '../pages/DashboardPage';
import { Given, Then, When } from '../support/fixtures';
import type { SustainabilityRating } from '../support/supplier-api';

const RATINGS: SustainabilityRating[] = ['A', 'B', 'C', 'D', 'E'];

const list = (values: string) => values.split(',').map((value) => value.trim());

async function filterByText(dashboard: DashboardPage, text: string) {
  await dashboard.nameOrDunsFilter.fill(text);
}

async function filterByRatings(dashboard: DashboardPage, ratings: string) {
  for (const rating of list(ratings)) {
    await dashboard.ratingCheckbox(rating as SustainabilityRating).check();
  }
}

When('the user filters the results by {string}', async ({ dashboard }, text: string) => {
  await filterByText(dashboard, text);
});

Given('the user has filtered the results by {string}', async ({ dashboard }, text: string) => {
  await filterByText(dashboard, text);
});

When('the user filters the results by the countries {string}', async ({ dashboard }, countries: string) => {
  await dashboard.countryFilter.selectOption(list(countries));
});

When('the user filters the results by the ratings {string}', async ({ dashboard }, ratings: string) => {
  await filterByRatings(dashboard, ratings);
});

Given('the user has filtered the results by the ratings {string}', async ({ dashboard }, ratings: string) => {
  await filterByRatings(dashboard, ratings);
});

Then('the user is told that no suppliers on this page match the selected filters', async ({ dashboard }) => {
  await expect(dashboard.filteredOutMessage).toBeVisible();
  await expect(dashboard.resultsTable).toBeHidden();
});

Then('no filters are active', async ({ dashboard }) => {
  await expect(dashboard.nameOrDunsFilter).toHaveValue('');
  await expect(dashboard.countryFilter).toHaveValues([]);
  for (const rating of RATINGS) {
    await expect(dashboard.ratingCheckbox(rating)).not.toBeChecked();
  }
});
