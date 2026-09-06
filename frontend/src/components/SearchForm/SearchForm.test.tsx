import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SearchForm } from './SearchForm';

describe('SearchForm', () => {
  it('calls onSearch with the numeric amount when valid', async () => {
    const onSearch = vi.fn();
    render(<SearchForm onSearch={onSearch} />);

    await userEvent.type(screen.getByLabelText(/order amount/i), '500');
    await userEvent.click(screen.getByRole('button', { name: /search/i }));

    expect(onSearch).toHaveBeenCalledWith(500);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('rejects an amount below the minimum without calling onSearch', async () => {
    const onSearch = vi.fn();
    render(<SearchForm onSearch={onSearch} />);

    await userEvent.type(screen.getByLabelText(/order amount/i), '100');
    await userEvent.click(screen.getByRole('button', { name: /search/i }));

    expect(onSearch).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent(/at least 250/i);
  });

  it('rejects an empty amount without calling onSearch', async () => {
    const onSearch = vi.fn();
    render(<SearchForm onSearch={onSearch} />);

    await userEvent.click(screen.getByRole('button', { name: /search/i }));

    expect(onSearch).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });
});
