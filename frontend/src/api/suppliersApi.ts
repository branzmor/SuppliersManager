import type { PotentialSuppliersResponse } from '../types/supplier';

// TODO: implement — GET /suppliers/potential?rate=&limit=&offset=
// See wiki/itx-iop_tech-supplier_flow-main-openapi3_1.yaml operationId "potentialSuppliers".
// limit is capped server-side at 10 (OpenAPI QueryLimit.maximum) — the pagination UI must respect
// that rather than letting the user request a larger page.
export async function getPotentialSuppliers(
  _rate: number,
  _limit: number,
  _offset: number,
): Promise<PotentialSuppliersResponse> {
  throw new Error('TODO: implement getPotentialSuppliers');
}
