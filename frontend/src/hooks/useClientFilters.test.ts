import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useClientFilters } from './useClientFilters';
import type { PotentialSupplier } from '../types/supplier';

const rows: PotentialSupplier[] = [
  { duns: 111111111, name: 'Acme', country: 'ES', annualTurnover: 200000, status: 'Active', sustainabilityRating: 'A', score: 20000 },
  { duns: 222222222, name: 'Beta Corp', country: 'PT', annualTurnover: 300000, status: 'Active', sustainabilityRating: 'B', score: 15000 },
  { duns: 333333333, name: 'Gamma', country: 'ES', annualTurnover: 400000, status: 'Disqualified', sustainabilityRating: 'C', score: 10000 },
];

describe('useClientFilters', () => {
  it('returns all rows unfiltered by default', () => {
    const { result } = renderHook(() => useClientFilters());
    expect(result.current.apply(rows)).toHaveLength(3);
  });

  it('filters by name, case-insensitively', () => {
    const { result } = renderHook(() => useClientFilters());
    act(() => result.current.setSearchTerm('beta'));
    expect(result.current.apply(rows).map((r) => r.duns)).toEqual([222222222]);
  });

  it('filters by DUNS substring', () => {
    const { result } = renderHook(() => useClientFilters());
    act(() => result.current.setSearchTerm('333333333'));
    expect(result.current.apply(rows).map((r) => r.duns)).toEqual([333333333]);
  });

  it('filters by selected countries', () => {
    const { result } = renderHook(() => useClientFilters());
    act(() => result.current.setSelectedCountries(['PT']));
    expect(result.current.apply(rows).map((r) => r.duns)).toEqual([222222222]);
  });

  it('filters by selected ratings', () => {
    const { result } = renderHook(() => useClientFilters());
    act(() => result.current.setSelectedRatings(['C']));
    expect(result.current.apply(rows).map((r) => r.duns)).toEqual([333333333]);
  });

  it('combines search, country and rating filters', () => {
    const { result } = renderHook(() => useClientFilters());
    act(() => {
      result.current.setSelectedCountries(['ES']);
      result.current.setSelectedRatings(['A']);
    });
    expect(result.current.apply(rows).map((r) => r.duns)).toEqual([111111111]);
  });

  it('reports hasActiveFilters false by default and true once any filter is set', () => {
    const { result } = renderHook(() => useClientFilters());
    expect(result.current.hasActiveFilters).toBe(false);

    act(() => result.current.setSearchTerm('beta'));
    expect(result.current.hasActiveFilters).toBe(true);
  });

  it('reset clears every filter and hasActiveFilters goes back to false', () => {
    const { result } = renderHook(() => useClientFilters());
    act(() => {
      result.current.setSearchTerm('beta');
      result.current.setSelectedCountries(['PT']);
      result.current.setSelectedRatings(['B']);
    });
    expect(result.current.hasActiveFilters).toBe(true);

    act(() => result.current.reset());

    expect(result.current.searchTerm).toBe('');
    expect(result.current.selectedCountries).toEqual([]);
    expect(result.current.selectedRatings).toEqual([]);
    expect(result.current.hasActiveFilters).toBe(false);
    expect(result.current.apply(rows)).toHaveLength(3);
  });
});
