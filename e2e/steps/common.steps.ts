import { expect } from '@playwright/test';
import { Given } from '../support/fixtures';
import { REFERENCE_SUPPLIERS } from '../support/reference-suppliers';

Given('the reference supplier catalogue is available', async ({ supplierApi }) => {
  // Seeded once in global-setup; re-checked here so a scenario fails on its real precondition
  // rather than on a confusing table mismatch if the catalogue was ever altered.
  const suppliers = await Promise.all(REFERENCE_SUPPLIERS.map(({ duns }) => supplierApi.getSupplier(duns)));
  expect(suppliers).toEqual(REFERENCE_SUPPLIERS.map((supplier) => ({ ...supplier, status: 'Active' })));
});

Given('the user is on the potential suppliers dashboard', async ({ dashboard }) => {
  await dashboard.open();
});
