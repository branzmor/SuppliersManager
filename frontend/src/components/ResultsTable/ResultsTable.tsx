import type { PotentialSupplier } from '../../types/supplier';
import { formatCurrencyEur, formatScore } from '../../utils/formatters';

export interface ResultsTableProps {
  suppliers: PotentialSupplier[];
  sortColumn: string;
  sortDirection: 'asc' | 'desc';
  onSortChange: (column: string) => void;
}

interface ColumnDef {
  key: keyof PotentialSupplier;
  label: string;
}

const COLUMNS: ColumnDef[] = [
  { key: 'duns', label: 'DUNS' },
  { key: 'name', label: 'Name' },
  { key: 'country', label: 'Country' },
  { key: 'annualTurnover', label: 'Annual Turnover' },
  { key: 'sustainabilityRating', label: 'Rating' },
  { key: 'score', label: 'Score' },
];

// Rendering the empty state is the caller's responsibility (see Dashboard) — this component
// always renders a table shell, so it can also be used to show a "no results" row seamlessly if
// that ever changes, without touching the sorting/header logic below.
export function ResultsTable({ suppliers, sortColumn, sortDirection, onSortChange }: ResultsTableProps) {
  return (
    <table className="results-table">
      <thead>
        <tr>
          {COLUMNS.map((column) => {
            const isActive = column.key === sortColumn;
            const ariaSort = isActive ? (sortDirection === 'asc' ? 'ascending' : 'descending') : 'none';
            const indicator = isActive ? (sortDirection === 'asc' ? '▲' : '▼') : '';
            return (
              <th key={column.key} aria-sort={ariaSort}>
                <button type="button" className="results-table__sort-button" onClick={() => onSortChange(column.key)}>
                  {column.label}{' '}
                  <span className="results-table__sort-indicator" aria-hidden="true">
                    {indicator}
                  </span>
                  {isActive && (
                    <span className="visually-hidden">
                      , sorted {sortDirection === 'asc' ? 'ascending' : 'descending'}
                    </span>
                  )}
                </button>
              </th>
            );
          })}
        </tr>
      </thead>
      <tbody>
        {suppliers.map((supplier) => (
          <tr key={supplier.duns}>
            <td>{supplier.duns}</td>
            <td>{supplier.name}</td>
            <td>{supplier.country}</td>
            <td>{formatCurrencyEur(supplier.annualTurnover)}</td>
            <td>{supplier.sustainabilityRating}</td>
            <td>{formatScore(supplier.score)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
