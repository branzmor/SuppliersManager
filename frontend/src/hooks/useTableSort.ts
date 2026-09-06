import { useState } from 'react';
import type { PotentialSupplier } from '../types/supplier';

export type SortDirection = 'asc' | 'desc';
export type SortableColumn = keyof PotentialSupplier;

export interface UseTableSortResult {
  sortColumn: SortableColumn;
  sortDirection: SortDirection;
  toggleSort: (column: SortableColumn) => void;
  sort: (rows: PotentialSupplier[]) => PotentialSupplier[];
}

export function useTableSort(): UseTableSortResult {
  const [sortColumn, setSortColumn] = useState<SortableColumn>('score');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  const toggleSort = (column: SortableColumn) => {
    if (column === sortColumn) {
      setSortDirection((direction) => (direction === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortColumn(column);
      setSortDirection('asc');
    }
  };

  const sort = (rows: PotentialSupplier[]) => {
    const sorted = [...rows].sort((a, b) => {
      const valueA = a[sortColumn];
      const valueB = b[sortColumn];

      let comparison: number;
      if (typeof valueA === 'number' && typeof valueB === 'number') {
        comparison = valueA - valueB;
      } else {
        comparison = String(valueA).localeCompare(String(valueB));
      }

      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return sorted;
  };

  return { sortColumn, sortDirection, toggleSort, sort };
}
