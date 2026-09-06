import { useState } from 'react';
import type { PotentialSupplier } from '../types/supplier';

export interface UseClientFiltersResult {
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  selectedCountries: string[];
  setSelectedCountries: (countries: string[]) => void;
  selectedRatings: string[];
  setSelectedRatings: (ratings: string[]) => void;
  hasActiveFilters: boolean;
  reset: () => void;
  apply: (rows: PotentialSupplier[]) => PotentialSupplier[];
}

export function useClientFilters(): UseClientFiltersResult {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCountries, setSelectedCountries] = useState<string[]>([]);
  const [selectedRatings, setSelectedRatings] = useState<string[]>([]);

  const hasActiveFilters = searchTerm.trim().length > 0 || selectedCountries.length > 0 || selectedRatings.length > 0;

  // Clears every filter - must be called whenever a new search (a different amount) starts, so
  // filters left over from the previous result page can't silently hide rows of the new one
  // (which would otherwise look like a false "no suppliers match" empty state).
  const reset = () => {
    setSearchTerm('');
    setSelectedCountries([]);
    setSelectedRatings([]);
  };

  const apply = (rows: PotentialSupplier[]) => {
    const term = searchTerm.trim().toLowerCase();

    return rows.filter((row) => {
      const matchesTerm =
        term.length === 0 ||
        row.name.toLowerCase().includes(term) ||
        String(row.duns).includes(term);
      const matchesCountry = selectedCountries.length === 0 || selectedCountries.includes(row.country);
      const matchesRating = selectedRatings.length === 0 || selectedRatings.includes(row.sustainabilityRating);
      return matchesTerm && matchesCountry && matchesRating;
    });
  };

  return {
    searchTerm,
    setSearchTerm,
    selectedCountries,
    setSelectedCountries,
    selectedRatings,
    setSelectedRatings,
    hasActiveFilters,
    reset,
    apply,
  };
}
