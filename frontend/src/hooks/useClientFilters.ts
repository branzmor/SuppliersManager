import { useState } from 'react';
import type { PotentialSupplier } from '../types/supplier';

export interface UseClientFiltersResult {
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  selectedCountries: string[];
  setSelectedCountries: (countries: string[]) => void;
  selectedRatings: string[];
  setSelectedRatings: (ratings: string[]) => void;
  apply: (rows: PotentialSupplier[]) => PotentialSupplier[];
}

// TODO: implement client-side filtering (README: "Client-side search" by name/DUNS, "Country
// filter", "Rating filter"). These filter the already-fetched page client-side — they do not
// re-trigger the backend call, unlike rate/limit/offset which do.
export function useClientFilters(): UseClientFiltersResult {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCountries, setSelectedCountries] = useState<string[]>([]);
  const [selectedRatings, setSelectedRatings] = useState<string[]>([]);

  const apply = (rows: PotentialSupplier[]) => rows;

  return {
    searchTerm,
    setSearchTerm,
    selectedCountries,
    setSelectedCountries,
    selectedRatings,
    setSelectedRatings,
    apply,
  };
}
