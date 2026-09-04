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

export function useClientFilters(): UseClientFiltersResult {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCountries, setSelectedCountries] = useState<string[]>([]);
  const [selectedRatings, setSelectedRatings] = useState<string[]>([]);

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
    apply,
  };
}
