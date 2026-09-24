import { randomInt } from 'node:crypto';
import type { APIResponse } from '@playwright/test';
import type { Candidate } from './supplier-api';

// Far above the reference catalogue (<= 90M) so these suppliers never show up in its scenarios'
// searches - which run first anyway (the lifecycle project depends on the dashboard project).
const LIFECYCLE_TURNOVER_BASE = 1_000_000_000_000;

/**
 * State owned by a single scenario. Suppliers are addressed in the Gherkin by name; each gets a
 * unique random DUNS (the reference catalogue uses 9000000xx) and a turnover that grows with
 * creation time, so searching for "turnover - 1" only lists that supplier plus those created at
 * the same moment by parallel scenarios - always few enough to fit on one result page.
 */
export class ScenarioState {
  private readonly suppliers = new Map<string, Candidate>();
  lastApiResponse?: APIResponse;

  createSupplier(name: string, country: string): Candidate {
    const candidate: Candidate = {
      duns: randomInt(800_000_000, 900_000_000),
      name,
      country,
      annualTurnover: LIFECYCLE_TURNOVER_BASE + Date.now(),
    };
    this.suppliers.set(name, candidate);
    return candidate;
  }

  supplier(name: string): Candidate {
    const candidate = this.suppliers.get(name);
    if (!candidate) {
      throw new Error(`"${name}" has not been created in this scenario`);
    }
    return candidate;
  }
}
