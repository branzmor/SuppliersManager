import { apiGet } from './client';
import type { PotentialSuppliersResponse } from '../types/supplier';

export async function getPotentialSuppliers(
  rate: number,
  limit: number,
  offset: number,
  signal?: AbortSignal,
): Promise<PotentialSuppliersResponse> {
  return apiGet<PotentialSuppliersResponse>('/suppliers/potential', { rate, limit, offset }, signal);
}
