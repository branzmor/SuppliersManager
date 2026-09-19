// Types mirrored 1:1 from wiki/iop_tech-supplier_flow-main-openapi3_1.yaml.
// Kept separate from any UI-specific view types (add those in the component/hook that needs them,
// not here).

export type SustainabilityRating = 'A' | 'B' | 'C' | 'D' | 'E';

// The API's Supplier.status enum only ever contains these two values — ON_PROBATION is
// intentionally not representable here, see backend SOLUTION.md "API pública vs. estado interno".
export type SupplierStatus = 'Active' | 'Disqualified';

export interface PotentialSupplier {
  duns: number;
  name: string;
  country: string;
  annualTurnover: number;
  status: SupplierStatus;
  sustainabilityRating: SustainabilityRating;
  score: number;
}

export interface Pagination {
  limit: number;
  offset: number;
  total: number;
}

export interface PotentialSuppliersResponse {
  data: PotentialSupplier[];
  pagination: Pagination;
}

export interface ApiError {
  info: string;
}
