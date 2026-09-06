import { useMemo, useState } from 'react';
import { SearchForm } from '../../components/SearchForm/SearchForm';
import { LoadingIndicator } from '../../components/LoadingIndicator/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage/ErrorMessage';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { FiltersBar } from '../../components/FiltersBar/FiltersBar';
import { ResultsTable } from '../../components/ResultsTable/ResultsTable';
import { Pagination } from '../../components/Pagination/Pagination';
import { usePotentialSuppliers } from '../../hooks/usePotentialSuppliers';
import { useClientFilters } from '../../hooks/useClientFilters';
import { useTableSort } from '../../hooks/useTableSort';

const PAGE_SIZE = 10;

export function Dashboard() {
  const [rate, setRate] = useState<number | null>(null);
  const [offset, setOffset] = useState(0);

  const { suppliers, total, isLoading, error, hasSearched, search } = usePotentialSuppliers();
  const filters = useClientFilters();
  const { sortColumn, sortDirection, toggleSort, sort } = useTableSort();

  const handleSearch = (newRate: number) => {
    setRate(newRate);
    setOffset(0);
    // A new amount means a brand-new result set - filters left over from the previous search
    // (a country/rating/text filter that happens to match nothing in the new page) must not
    // silently produce a false "no suppliers match" empty state.
    filters.reset();
    void search(newRate, PAGE_SIZE, 0);
  };

  const handlePageChange = (newOffset: number) => {
    if (rate === null) return;
    setOffset(newOffset);
    void search(rate, PAGE_SIZE, newOffset);
  };

  const availableCountries = useMemo(
    () => Array.from(new Set(suppliers.map((supplier) => supplier.country))).sort(),
    [suppliers],
  );

  const visibleSuppliers = useMemo(
    () => sort(filters.apply(suppliers)),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [suppliers, filters.searchTerm, filters.selectedCountries, filters.selectedRatings, sortColumn, sortDirection],
  );

  return (
    <div className="dashboard">
      <h1 className="dashboard__title">Potential Suppliers</h1>
      <SearchForm onSearch={handleSearch} />

      {isLoading && <LoadingIndicator />}
      {!isLoading && error && <ErrorMessage message={error} />}

      {!isLoading && !error && hasSearched && (
        <>
          {suppliers.length === 0 ? (
            <EmptyState />
          ) : (
            <>
              <FiltersBar
                availableCountries={availableCountries}
                selectedCountries={filters.selectedCountries}
                onCountriesChange={filters.setSelectedCountries}
                selectedRatings={filters.selectedRatings}
                onRatingsChange={filters.setSelectedRatings}
                searchTerm={filters.searchTerm}
                onSearchTermChange={filters.setSearchTerm}
              />
              {/* The server page came back non-empty, but the client-side name/DUNS/country/
                  rating filters above may still narrow it down to nothing — that's a distinct
                  empty case from "the search itself returned zero suppliers" and needs its own
                  message, or the user would just see a table with headers and no rows. */}
              {visibleSuppliers.length === 0 ? (
                <EmptyState />
              ) : (
                <ResultsTable
                  suppliers={visibleSuppliers}
                  sortColumn={sortColumn}
                  sortDirection={sortDirection}
                  onSortChange={(column) => toggleSort(column as typeof sortColumn)}
                />
              )}
              <Pagination
                limit={PAGE_SIZE}
                offset={offset}
                total={total}
                onPageChange={handlePageChange}
                visibleCount={visibleSuppliers.length}
                hasActiveFilters={filters.hasActiveFilters}
              />
            </>
          )}
        </>
      )}
    </div>
  );
}
