import { expect, type Locator, type Page } from '@playwright/test';
import type { SustainabilityRating } from '../support/supplier-api';

export type SortOrder = 'ascending' | 'descending';

/** Column header label -> expected cell text, e.g. { Name: 'Milano Moda', Score: '3.281.250,00' }. */
export type ExpectedRow = Record<string, string>;

/**
 * The potential suppliers dashboard - the application's only page. Locators are user-facing
 * (roles, labels, visible text) so the steps read like what a user perceives, and survive markup
 * or styling changes.
 */
export class DashboardPage {
  readonly heading: Locator;
  readonly amountInput: Locator;
  readonly searchButton: Locator;
  readonly alert: Locator;
  readonly loadingIndicator: Locator;
  readonly noResultsMessage: Locator;
  readonly filteredOutMessage: Locator;
  readonly resultsTable: Locator;
  readonly resultsSummary: Locator;
  readonly nameOrDunsFilter: Locator;
  readonly countryFilter: Locator;
  readonly ratingFilter: Locator;
  readonly pageIndicator: Locator;
  readonly previousPageButton: Locator;
  readonly nextPageButton: Locator;

  constructor(private readonly page: Page) {
    this.heading = page.getByRole('heading', { name: 'Potential Suppliers' });
    this.amountInput = page.getByLabel('Order amount (€)');
    this.searchButton = page.getByRole('button', { name: 'Search', exact: true });
    this.alert = page.getByRole('alert');
    this.loadingIndicator = page.getByRole('status').filter({ hasText: 'Loading suppliers' });
    this.noResultsMessage = page.getByText('No potential suppliers found for this order amount.');
    this.filteredOutMessage = page.getByText('No suppliers on this page match the selected filters.');
    this.resultsTable = page.getByRole('table');
    this.resultsSummary = page.getByText(/^\d+ (suppliers found|visible suppliers out of \d+ total)$/);
    this.nameOrDunsFilter = page.getByRole('textbox', { name: 'Search' });
    this.countryFilter = page.getByRole('listbox', { name: 'Country' });
    this.ratingFilter = page.getByRole('group', { name: 'Rating' });
    this.pageIndicator = page.getByText(/^Page \d+ of \d+$/);
    this.previousPageButton = page.getByRole('button', { name: 'Previous' });
    this.nextPageButton = page.getByRole('button', { name: 'Next' });
  }

  async open(): Promise<void> {
    await this.page.goto('/');
    await expect(this.heading).toBeVisible();
  }

  async searchFor(amount: number): Promise<void> {
    await this.amountInput.fill(String(amount));
    await this.searchButton.click();
  }

  /** Resolves once a search has produced a result page or the "no suppliers" message. */
  async waitForSearchResult(): Promise<void> {
    await expect(this.resultsSummary.or(this.noResultsMessage)).toBeVisible();
  }

  get dataRows(): Locator {
    // Header cells are `columnheader`s, so this keeps only the body rows.
    return this.resultsTable.getByRole('row').filter({ has: this.page.getByRole('cell') });
  }

  rowFor(duns: number): Locator {
    return this.dataRows.filter({ has: this.page.getByRole('cell', { name: String(duns), exact: true }) });
  }

  columnHeader(label: string): Locator {
    return this.resultsTable.getByRole('columnheader', { name: new RegExp(`^${escapeRegExp(label)}\\b`) });
  }

  ratingCheckbox(rating: SustainabilityRating): Locator {
    return this.ratingFilter.getByRole('checkbox', { name: rating, exact: true });
  }

  async sortBy(label: string): Promise<void> {
    await this.columnHeader(label).getByRole('button').click();
  }

  async expectSortedBy(label: string, order: SortOrder): Promise<void> {
    await expect(this.columnHeader(label)).toHaveAttribute('aria-sort', order);
  }

  /** Asserts the exact rows on screen, in order, comparing only the columns present in `expected`. */
  async expectRows(expected: ExpectedRow[]): Promise<void> {
    await expect(this.dataRows).toHaveCount(expected.length);
    for (const [index, row] of expected.entries()) {
      await this.expectRow(this.dataRows.nth(index), row, `row ${index + 1}`);
    }
  }

  async expectRow(row: Locator, expected: ExpectedRow, description = 'row'): Promise<void> {
    const headers = await this.resultsTable.getByRole('columnheader').allInnerTexts();
    for (const [label, value] of Object.entries(expected)) {
      const column = headers.findIndex((header) => header.trim().startsWith(label));
      expect(column, `column "${label}" not found in [${headers.join(', ')}]`).toBeGreaterThanOrEqual(0);
      await expect(row.getByRole('cell').nth(column), `${description}, column "${label}"`).toHaveText(value);
    }
  }
}

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
