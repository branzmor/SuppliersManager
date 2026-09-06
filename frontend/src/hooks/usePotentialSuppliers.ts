import { useEffect, useRef, useState } from 'react';
import type { PotentialSupplier } from '../types/supplier';
import { getPotentialSuppliers } from '../api/suppliersApi';
import { ApiClientError } from '../api/client';

export interface UsePotentialSuppliersResult {
  suppliers: PotentialSupplier[];
  total: number;
  isLoading: boolean;
  error: string | null;
  hasSearched: boolean;
  search: (rate: number, limit: number, offset: number) => Promise<void>;
}

export function usePotentialSuppliers(): UsePotentialSuppliersResult {
  const [suppliers, setSuppliers] = useState<PotentialSupplier[]>([]);
  const [total, setTotal] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasSearched, setHasSearched] = useState(false);

  // Guards against out-of-order responses: searching again (a new amount or a page change)
  // aborts whatever request is still in flight, and every callback below checks its own
  // controller's `aborted` flag before touching state - so a superseded request can never
  // overwrite results from a request issued after it, no matter which one's network call
  // actually finishes first.
  const activeRequestRef = useRef<AbortController | null>(null);
  const isMountedRef = useRef(true);

  useEffect(
    () => () => {
      isMountedRef.current = false;
      activeRequestRef.current?.abort();
    },
    [],
  );

  const search = async (rate: number, limit: number, offset: number) => {
    activeRequestRef.current?.abort();
    const controller = new AbortController();
    activeRequestRef.current = controller;

    setIsLoading(true);
    setError(null);
    try {
      const response = await getPotentialSuppliers(rate, limit, offset, controller.signal);
      if (controller.signal.aborted || !isMountedRef.current) {
        return;
      }
      setSuppliers(response.data);
      setTotal(response.pagination.total);
    } catch (err) {
      // A cancelled request is not a user-facing error - it was superseded by a newer search
      // or the component unmounted, either way there is nothing to report.
      if (controller.signal.aborted || !isMountedRef.current) {
        return;
      }
      const message =
        err instanceof ApiClientError ? err.message : 'Something went wrong while fetching suppliers. Please try again.';
      setError(message);
      setSuppliers([]);
      setTotal(0);
    } finally {
      if (!controller.signal.aborted && isMountedRef.current) {
        setIsLoading(false);
        setHasSearched(true);
      }
    }
  };

  return { suppliers, total, isLoading, error, hasSearched, search };
}
