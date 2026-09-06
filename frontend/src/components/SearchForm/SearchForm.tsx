import { useState } from 'react';
import type { FormEvent } from 'react';

const MIN_RATE = 250;

export interface SearchFormProps {
  onSearch: (rate: number) => void;
}

export function SearchForm({ onSearch }: SearchFormProps) {
  const [amount, setAmount] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();

    const rate = Number(amount);
    if (amount.trim() === '' || Number.isNaN(rate) || rate < MIN_RATE) {
      setValidationError(`Amount must be at least ${MIN_RATE}`);
      return;
    }

    setValidationError(null);
    onSearch(rate);
  };

  return (
    // noValidate: the browser's own "value must be >= 250" tooltip (triggered by the `min`
    // attribute below) would otherwise block the submit event before our handler ever runs,
    // silently swallowing the custom validation message the README asks for.
    <form className="search-form" onSubmit={handleSubmit} noValidate>
      <div className="search-form__field">
        <label htmlFor="rate-input">Order amount (€)</label>
        <input
          id="rate-input"
          type="number"
          min={MIN_RATE}
          step="1"
          value={amount}
          onChange={(event) => setAmount(event.target.value)}
          placeholder={`Minimum ${MIN_RATE}`}
        />
      </div>
      <button type="submit" className="search-form__submit">
        Search
      </button>
      {validationError && (
        <p className="search-form__error" role="alert">
          {validationError}
        </p>
      )}
    </form>
  );
}
