export interface PaginationProps {
  limit: number;
  offset: number;
  total: number;
  onPageChange: (newOffset: number) => void;
}

export function Pagination({ limit, offset, total, onPageChange }: PaginationProps) {
  const currentPage = Math.floor(offset / limit) + 1;
  const totalPages = Math.max(1, Math.ceil(total / limit));
  const hasPrevious = offset > 0;
  const hasNext = offset + limit < total;

  return (
    <div className="pagination">
      <span className="pagination__count">{total} suppliers found</span>
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
