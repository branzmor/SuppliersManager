export interface FiltersBarProps {
  availableCountries: string[];
  selectedCountries: string[];
  onCountriesChange: (countries: string[]) => void;
  selectedRatings: string[];
  onRatingsChange: (ratings: string[]) => void;
  searchTerm: string;
  onSearchTermChange: (term: string) => void;
}

// TODO: implement — free-text search by name/DUNS, country dropdown/multi-select, rating
// (A-E) filter, per README frontend requirements table.
export function FiltersBar(_props: FiltersBarProps) {
  return null;
}
