import type { PotentialSupplier } from '../../types/supplier';

export interface ResultsTableProps {
  suppliers: PotentialSupplier[];
  sortColumn: string;
  sortDirection: 'asc' | 'desc';
  onSortChange: (column: string) => void;
}

// TODO: implement — columns per README: DUNS, Name, Country, Annual Turnover (€, thousand
// separators, via utils/formatters#formatCurrencyEur), Sustainability Rating, Score (2 decimals,
// via utils/formatters#formatScore). Clicking a header calls onSortChange (README: "Column
// sorting"). Renders EmptyState when suppliers is empty (handled by the parent, or here — decide
// and document when implementing).
export function ResultsTable(_props: ResultsTableProps) {
  return null;
}
