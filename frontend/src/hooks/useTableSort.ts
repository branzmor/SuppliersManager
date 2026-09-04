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

// TODO: implement column-header click sorting (README: "Column sorting... toggling
// ascending/descending"). Default should be score/descending per README "Default sort".
export function useTableSort(): UseTableSortResult {
  const [sortColumn] = useState<SortableColumn>('score');
  const [sortDirection] = useState<SortDirection>('desc');

  const toggleSort = (_column: SortableColumn) => {
    throw new Error('TODO: implement toggleSort');
  };

  const sort = (rows: PotentialSupplier[]) => rows;

  return { sortColumn, sortDirection, toggleSort, sort };
}
