import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useTableSort } from './useTableSort';
import type { PotentialSupplier } from '../types/supplier';

const rows: PotentialSupplier[] = [
  { duns: 1, name: 'Beta', country: 'ES', annualTurnover: 300000, status: 'Active', sustainabilityRating: 'B', score: 15000 },
  { duns: 2, name: 'Alpha', country: 'PT', annualTurnover: 200000, status: 'Active', sustainabilityRating: 'A', score: 26250 },
  { duns: 3, name: 'Gamma', country: 'ES', annualTurnover: 400000, status: 'Active', sustainabilityRating: 'C', score: 10000 },
];

describe('useTableSort', () => {
  it('defaults to score descending', () => {
    const { result } = renderHook(() => useTableSort());
    expect(result.current.sortColumn).toBe('score');
    expect(result.current.sortDirection).toBe('desc');
    expect(result.current.sort(rows).map((r) => r.duns)).toEqual([2, 1, 3]);
  });

  it('toggles a new column to ascending first', () => {
    const { result } = renderHook(() => useTableSort());
    act(() => result.current.toggleSort('name'));
    expect(result.current.sortColumn).toBe('name');
    expect(result.current.sortDirection).toBe('asc');
    expect(result.current.sort(rows).map((r) => r.name)).toEqual(['Alpha', 'Beta', 'Gamma']);
  });

  it('toggling the same column again flips direction', () => {
    const { result } = renderHook(() => useTableSort());
    act(() => result.current.toggleSort('name'));
    act(() => result.current.toggleSort('name'));
    expect(result.current.sortDirection).toBe('desc');
    expect(result.current.sort(rows).map((r) => r.name)).toEqual(['Gamma', 'Beta', 'Alpha']);
  });

  it('sorts numeric columns numerically, not lexicographically', () => {
    const { result } = renderHook(() => useTableSort());
    act(() => result.current.toggleSort('annualTurnover'));
    expect(result.current.sort(rows).map((r) => r.annualTurnover)).toEqual([200000, 300000, 400000]);
  });
});
