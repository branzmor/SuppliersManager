import { useState } from 'react';
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

  const search = async (rate: number, limit: number, offset: number) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await getPotentialSuppliers(rate, limit, offset);
      setSuppliers(response.data);
      setTotal(response.pagination.total);
    } catch (err) {
      const message =
        err instanceof ApiClientError ? err.message : 'Something went wrong while fetching suppliers. Please try again.';
      setError(message);
      setSuppliers([]);
      setTotal(0);
    } finally {
      setIsLoading(false);
      setHasSearched(true);
    }
  };

  return { suppliers, total, isLoading, error, hasSearched, search };
}
