import { expect, type APIRequestContext, type APIResponse } from '@playwright/test';

export type SustainabilityRating = 'A' | 'B' | 'C' | 'D' | 'E';
export type SupplierStatus = 'Active' | 'Disqualified';

export interface Candidate {
  duns: number;
  name: string;
  country: string;
  annualTurnover: number;
}

export interface Supplier extends Candidate {
  status: SupplierStatus;
  sustainabilityRating: SustainabilityRating;
}

/**
 * Thin client over the backend's public REST API (wiki/iop_tech-supplier_flow-main-openapi3_1.yaml),
 * used to arrange state the dashboard has no UI for: the candidacy lifecycle is a supervisor
 * operation exposed only through the API.
 */
export class SupplierApi {
  constructor(private readonly request: APIRequestContext) {}

  async registerCandidate(candidate: Candidate): Promise<void> {
    await expectStatus(await this.request.post('/candidates', { data: candidate }), 201);
  }

  async acceptCandidate(duns: number, sustainabilityRating: SustainabilityRating): Promise<void> {
    await expectStatus(await this.tryAcceptCandidate(duns, sustainabilityRating), 204);
  }

  /** Returns the raw response so callers can assert on a rejected acceptance. */
  tryAcceptCandidate(duns: number, sustainabilityRating: SustainabilityRating): Promise<APIResponse> {
    return this.request.post(`/candidates/${duns}/accept`, { data: { sustainabilityRating } });
  }

  async refuseCandidate(duns: number): Promise<void> {
    await expectStatus(await this.request.post(`/candidates/${duns}/refuse`), 204);
  }

  async banSupplier(duns: number): Promise<void> {
    await expectStatus(await this.request.post(`/suppliers/${duns}/ban`), 204);
  }

  async getSupplier(duns: number): Promise<Supplier> {
    const response = await this.request.get(`/suppliers/${duns}`);
    await expectStatus(response, 200);
    return (await response.json()) as Supplier;
  }
}

async function expectStatus(response: APIResponse, status: number): Promise<void> {
  expect(response.status(), `${response.url()} -> ${await response.text()}`).toBe(status);
}
