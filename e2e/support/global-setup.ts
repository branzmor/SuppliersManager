import { request } from '@playwright/test';
import { API_URL } from './env';
import { startStack } from './stack';
import { SupplierApi } from './supplier-api';
import { REFERENCE_SUPPLIERS } from './reference-suppliers';

export default async function globalSetup(): Promise<void> {
  startStack();

  const context = await request.newContext({ baseURL: API_URL });
  try {
    const api = new SupplierApi(context);
    // Seeded through the public API (never straight into the database) so the data goes through
    // the same validation, country check and state machine as real traffic.
    await Promise.all(
      REFERENCE_SUPPLIERS.map(async (supplier) => {
        await api.registerCandidate(supplier);
        await api.acceptCandidate(supplier.duns, supplier.sustainabilityRating);
      }),
    );
  } finally {
    await context.dispose();
  }
}
