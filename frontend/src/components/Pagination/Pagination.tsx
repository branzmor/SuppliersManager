export interface PaginationProps {
  limit: number;
  offset: number;
  total: number;
  onPageChange: (newOffset: number) => void;
  /** Rows left after client-side filters (name/DUNS/country/rating) are applied to this page. */
  visibleCount: number;
  /** Whether any client-side filter is currently active. */
  hasActiveFilters: boolean;
}

export function Pagination({ limit, offset, total, onPageChange, visibleCount, hasActiveFilters }: PaginationProps) {
  const currentPage = Math.floor(offset / limit) + 1;
  const totalPages = Math.max(1, Math.ceil(total / limit));
  const hasPrevious = offset > 0;
  const hasNext = offset + limit < total;

  return (
    <div className="pagination">
      <span className="pagination__count">
        {/* The server-reported total describes the whole matching dataset; visibleCount is only
            what survives the client-side filters on THIS loaded page - the two are not the same
            number and must not be presented as if they were (see SOLUTION.md). */}
        {hasActiveFilters ? `${visibleCount} visible suppliers out of ${total} total` : `${total} suppliers found`}
      </span>
      <div className="pagination__controls">
        <button
          type="button"
          disabled={!hasPrevious}
          onClick={() => onPageChange(Math.max(0, offset - limit))}
        >
          Previous
        </button>
        <span className="pagination__page">
          Page {currentPage} of {totalPages}
        </span>
        <button type="button" disabled={!hasNext} onClick={() => onPageChange(offset + limit)}>
          Next
        </button>
      </div>
    </div>
  );
}
