import type { ChangeEvent } from 'react';

const RATINGS = ['A', 'B', 'C', 'D', 'E'];

export interface FiltersBarProps {
  availableCountries: string[];
  selectedCountries: string[];
  onCountriesChange: (countries: string[]) => void;
  selectedRatings: string[];
  onRatingsChange: (ratings: string[]) => void;
  searchTerm: string;
  onSearchTermChange: (term: string) => void;
}

export function FiltersBar({
  availableCountries,
  selectedCountries,
  onCountriesChange,
  selectedRatings,
  onRatingsChange,
  searchTerm,
  onSearchTermChange,
}: FiltersBarProps) {
  const handleCountriesChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const values = Array.from(event.target.selectedOptions).map((option) => option.value);
    onCountriesChange(values);
  };

  const toggleRating = (rating: string) => {
    if (selectedRatings.includes(rating)) {
      onRatingsChange(selectedRatings.filter((value) => value !== rating));
    } else {
      onRatingsChange([...selectedRatings, rating]);
    }
  };

  return (
    <div className="filters-bar">
      <div className="filters-bar__field">
        <label htmlFor="filter-search">Search</label>
        <input
          id="filter-search"
          type="text"
          placeholder="Filter by name or DUNS"
          value={searchTerm}
          onChange={(event) => onSearchTermChange(event.target.value)}
        />
      </div>

      <div className="filters-bar__field">
        <label htmlFor="filter-country">Country</label>
        <select
          id="filter-country"
          multiple
          value={selectedCountries}
          onChange={handleCountriesChange}
        >
          {availableCountries.map((country) => (
            <option key={country} value={country}>
              {country}
            </option>
          ))}
        </select>
      </div>

      <div className="filters-bar__field">
        <span className="filters-bar__label">Rating</span>
        <div className="filters-bar__ratings">
          {RATINGS.map((rating) => (
            <label key={rating} className="filters-bar__rating-option">
              <input
                type="checkbox"
                checked={selectedRatings.includes(rating)}
                onChange={() => toggleRating(rating)}
              />
              {rating}
            </label>
          ))}
        </div>
      </div>
    </div>
  );
}
