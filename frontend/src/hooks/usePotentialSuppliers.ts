import { useState } from 'react';
import type { PotentialSupplier } from '../types/supplier';

export interface UsePotentialSuppliersResult {
  suppliers: PotentialSupplier[];
  total: number;
  isLoading: boolean;
  error: string | null;
  search: (rate: number, limit: number, offset: number) => Promise<void>;
}

// TODO: implement — owns the fetch lifecycle (loading/error/data) for GET /suppliers/potential.
// Should call api/suppliersApi.getPotentialSuppliers and translate ApiClientError into a
// user-friendly message for the "Error handling" requirement.
export function usePotentialSuppliers(): UsePotentialSuppliersResult {
  const [suppliers] = useState<PotentialSupplier[]>([]);
  const [total] = useState(0);
  const [isLoading] = useState(false);
  const [error] = useState<string | null>(null);

  const search = async (_rate: number, _limit: number, _offset: number) => {
    throw new Error('TODO: implement search');
  };

  return { suppliers, total, isLoading, error, search };
}
