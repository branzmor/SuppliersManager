export interface PaginationProps {
  limit: number;
  offset: number;
  total: number;
  onPageChange: (newOffset: number) => void;
}

// TODO: implement — limit/offset-based pagination controls (README: "Pagination controls using
// limit and offset") plus the total result count (README: "Result count").
export function Pagination(_props: PaginationProps) {
  return null;
}
