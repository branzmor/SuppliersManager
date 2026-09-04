// Composition root for the "potential suppliers dashboard" (README §"Frontend").
//
// TODO: wire together, in order:
//   1. SearchForm (amount input, min 250) -> usePotentialSuppliers().search(rate, limit, offset)
//   2. LoadingIndicator while usePotentialSuppliers().isLoading
//   3. ErrorMessage when usePotentialSuppliers().error is set
//   4. EmptyState when the fetched page is empty
//   5. FiltersBar + useClientFilters() to filter the fetched page client-side (name/DUNS search,
//      country, rating)
//   6. useTableSort() to sort the filtered rows, defaulting to score descending
//   7. ResultsTable to render the sorted/filtered rows
//   8. Pagination (limit/offset) + result count, re-triggering usePotentialSuppliers().search
export function Dashboard() {
  return null;
}
