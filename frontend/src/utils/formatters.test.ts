import { describe, expect, it } from 'vitest';
import { formatCurrencyEur, formatScore } from './formatters';

// es-ES's Intl.NumberFormat separates the amount from the currency symbol with a non-breaking
// space (U+00A0), not a regular space — normalize before asserting so the test isn't coupled to
// that invisible detail.
function normalizeSpaces(value: string): string {
  return value.replace(/\s/g, ' ');
}

describe('formatCurrencyEur', () => {
  it('formats with thousand separators and the euro symbol', () => {
    expect(normalizeSpaces(formatCurrencyEur(1000000))).toBe('1.000.000 €');
  });

  it('formats small amounts', () => {
    expect(normalizeSpaces(formatCurrencyEur(250))).toBe('250 €');
  });
});

describe('formatScore', () => {
  it('always shows exactly two decimal places', () => {
    expect(formatScore(25000)).toBe('25.000,00');
  });

  it('rounds to two decimal places', () => {
    expect(formatScore(26250.005)).toBe('26.250,01');
  });
});
