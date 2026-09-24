import type { Candidate, SustainabilityRating } from './supplier-api';

export interface ReferenceSupplier extends Candidate {
  sustainabilityRating: SustainabilityRating;
}

/**
 * The fixed catalogue seeded once per run (see global-setup.ts) that the read-only dashboard
 * features assert against. Expected scores in the .feature files are derived from it:
 *
 *   score = turnover x 0.1 x rating constant (A=1, B=0.75, C=0.5, D=0.25, E=0.1)
 *           x 1.25 for the two lowest unique turnovers of each country.
 *
 * Every turnover is >= 10M EUR and every country is approved by the country-service mock
 * (wiremock/mappings: A-M approved). Lifecycle scenarios never create suppliers in these
 * countries, so the small-supplier bonus computed here can't be shifted by them.
 */
export const REFERENCE_SUPPLIERS: readonly ReferenceSupplier[] = [
  { duns: 900000001, name: 'Zippers & Buttons', country: 'ES', annualTurnover: 10_000_000, sustainabilityRating: 'A' },
  { duns: 900000002, name: 'Iberian Textiles', country: 'ES', annualTurnover: 20_000_000, sustainabilityRating: 'B' },
  { duns: 900000003, name: 'Cantabria Cotton', country: 'ES', annualTurnover: 40_000_000, sustainabilityRating: 'C' },
  { duns: 900000004, name: 'Galicia Knits', country: 'ES', annualTurnover: 80_000_000, sustainabilityRating: 'A' },
  { duns: 900000005, name: 'Lyon Silks', country: 'FR', annualTurnover: 12_000_000, sustainabilityRating: 'A' },
  { duns: 900000006, name: 'Paris Denim', country: 'FR', annualTurnover: 30_000_000, sustainabilityRating: 'D' },
  { duns: 900000007, name: 'Marseille Leather', country: 'FR', annualTurnover: 50_000_000, sustainabilityRating: 'B' },
  { duns: 900000008, name: 'Normandy Linen', country: 'FR', annualTurnover: 70_000_000, sustainabilityRating: 'E' },
  { duns: 900000009, name: 'Berlin Fasteners', country: 'DE', annualTurnover: 25_000_000, sustainabilityRating: 'C' },
  { duns: 900000010, name: 'Hamburg Threads', country: 'DE', annualTurnover: 60_000_000, sustainabilityRating: 'A' },
  { duns: 900000011, name: 'Milano Moda', country: 'IT', annualTurnover: 35_000_000, sustainabilityRating: 'B' },
  { duns: 900000012, name: 'Torino Wool', country: 'IT', annualTurnover: 90_000_000, sustainabilityRating: 'D' },
];
