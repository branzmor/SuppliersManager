import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { Dashboard } from './Dashboard';
import { getPotentialSuppliers } from '../../api/suppliersApi';
import { ApiClientError } from '../../api/client';
import type { PotentialSuppliersResponse, PotentialSupplier } from '../../types/supplier';

vi.mock('../../api/suppliersApi');

const mockedGetPotentialSuppliers = vi.mocked(getPotentialSuppliers);

function supplier(overrides: Partial<PotentialSupplier>): PotentialSupplier {
  return {
    duns: 100000001,
    name: 'Acme',
    country: 'ES',
    annualTurnover: 2_000_000,
    status: 'Active',
    sustainabilityRating: 'A',
    score: 20000,
    ...overrides,
  };
}

function response(data: PotentialSupplier[], total = data.length): PotentialSuppliersResponse {
  return { data, pagination: { limit: 10, offset: 0, total } };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

async function runSearch(user: ReturnType<typeof userEvent.setup>, amount: number | string) {
  const input = screen.getByLabelText(/order amount/i);
  await user.clear(input);
  await user.type(input, String(amount));
  await user.click(screen.getByRole('button', { name: /^search$/i }));
}

beforeEach(() => {
  mockedGetPotentialSuppliers.mockReset();
});

describe('Dashboard', () => {
  it('shows a loading indicator while the request is in flight', async () => {
    const user = userEvent.setup();
    const pending = deferred<PotentialSuppliersResponse>();
    mockedGetPotentialSuppliers.mockReturnValueOnce(pending.promise);
    render(<Dashboard />);

    await runSearch(user, 500);

    expect(screen.getByRole('status')).toBeInTheDocument();

    pending.resolve(response([supplier({})]));
    await waitFor(() => expect(screen.queryByRole('status')).not.toBeInTheDocument());
  });

  it('renders the results table on a successful response', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockResolvedValueOnce(
      response([supplier({ duns: 111111111, name: 'Acme' })]),
    );
    render(<Dashboard />);

    await runSearch(user, 500);

    expect(await screen.findByText('Acme')).toBeInTheDocument();
    expect(screen.getByText('111111111')).toBeInTheDocument();
  });

  it('shows a friendly error message when the API call fails', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockRejectedValueOnce(new ApiClientError('Something went wrong on the server.'));
    render(<Dashboard />);

    await runSearch(user, 500);

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong on the server.');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('shows the "no results" empty state when the backend returns zero suppliers', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockResolvedValueOnce(response([]));
    render(<Dashboard />);

    await runSearch(user, 5000);

    expect(
      await screen.findByText('No potential suppliers found for this order amount.'),
    ).toBeInTheDocument();
    // This is distinct from the filtered-out case below, so must never say "filters".
    expect(screen.queryByText(/selected filters/i)).not.toBeInTheDocument();
  });

  it('shows the "filtered out" empty state when client-side filters hide every row on a non-empty page', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockResolvedValueOnce(
      response([supplier({ duns: 800000001, name: 'Spanish Co', country: 'ES' })]),
    );
    render(<Dashboard />);
    await runSearch(user, 500);
    await screen.findByText('Spanish Co');

    // The server did return a supplier - the empty state below comes purely from the client-side
    // text filter hiding it, and must say so distinctly from a genuinely empty backend response.
    await user.type(screen.getByLabelText(/search/i), 'no such supplier name');

    expect(
      await screen.findByText('No suppliers on this page match the selected filters.'),
    ).toBeInTheDocument();
    expect(screen.queryByText(/for this order amount/i)).not.toBeInTheDocument();
    expect(screen.getByText('0 visible suppliers out of 1 total')).toBeInTheDocument();
  });

  it('supports paginating with limit/offset and shows the total count', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockResolvedValueOnce({
      data: [supplier({ duns: 200000001, name: 'Page One Supplier' })],
      pagination: { limit: 10, offset: 0, total: 11 },
    });
    render(<Dashboard />);
    await runSearch(user, 500);
    expect(await screen.findByText('Page One Supplier')).toBeInTheDocument();
    expect(screen.getByText('11 suppliers found')).toBeInTheDocument();

    mockedGetPotentialSuppliers.mockResolvedValueOnce({
      data: [supplier({ duns: 200000002, name: 'Page Two Supplier' })],
      pagination: { limit: 10, offset: 10, total: 11 },
    });
    await user.click(screen.getByRole('button', { name: /next/i }));

    expect(await screen.findByText('Page Two Supplier')).toBeInTheDocument();
    expect(mockedGetPotentialSuppliers).toHaveBeenLastCalledWith(500, 10, 10, expect.anything());
  });

  it('filters the loaded page client-side and shows the visible-vs-total count', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockResolvedValueOnce(
      response(
        [
          supplier({ duns: 300000001, name: 'Spanish Co', country: 'ES', sustainabilityRating: 'A' }),
          supplier({ duns: 300000002, name: 'French Co', country: 'FR', sustainabilityRating: 'B' }),
        ],
        2,
      ),
    );
    render(<Dashboard />);
    await runSearch(user, 500);
    expect(await screen.findByText('Spanish Co')).toBeInTheDocument();
    expect(screen.getByText('French Co')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/search/i), 'Spanish');

    expect(screen.getByText('Spanish Co')).toBeInTheDocument();
    expect(screen.queryByText('French Co')).not.toBeInTheDocument();
    expect(screen.getByText('1 visible suppliers out of 2 total')).toBeInTheDocument();
  });

  it('sorts by score descending by default', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockResolvedValueOnce(
      response([
        supplier({ duns: 400000001, name: 'Low Score', score: 100 }),
        supplier({ duns: 400000002, name: 'High Score', score: 900 }),
      ]),
    );
    render(<Dashboard />);
    await runSearch(user, 500);
    await screen.findByText('High Score');

    const rows = screen.getAllByRole('row').slice(1); // skip header row
    expect(within(rows[0]).getByText('High Score')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Low Score')).toBeInTheDocument();
  });

  it('toggles sort direction when a column header is clicked', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockResolvedValueOnce(
      response([
        supplier({ duns: 500000001, name: 'Beta', score: 100 }),
        supplier({ duns: 500000002, name: 'Alpha', score: 900 }),
      ]),
    );
    render(<Dashboard />);
    await runSearch(user, 500);
    await screen.findByText('Alpha');

    await user.click(screen.getByRole('button', { name: /^Name/ }));
    let rows = screen.getAllByRole('row').slice(1);
    expect(within(rows[0]).getByText('Alpha')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Beta')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /^Name/ }));
    rows = screen.getAllByRole('row').slice(1);
    expect(within(rows[0]).getByText('Beta')).toBeInTheDocument();
    expect(within(rows[1]).getByText('Alpha')).toBeInTheDocument();
  });

  it('ignores a stale response that resolves after a newer one', async () => {
    const user = userEvent.setup();
    const first = deferred<PotentialSuppliersResponse>();
    const second = deferred<PotentialSuppliersResponse>();
    mockedGetPotentialSuppliers.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
    render(<Dashboard />);

    await runSearch(user, 500);
    await runSearch(user, 999);

    // The second (newer) request resolves first; the first (older, superseded) request resolves
    // later - its stale data must never overwrite what the newer request already rendered.
    second.resolve(response([supplier({ duns: 600000002, name: 'Newer Result' })]));
    expect(await screen.findByText('Newer Result')).toBeInTheDocument();

    first.resolve(response([supplier({ duns: 600000001, name: 'Stale Result' })]));
    await new Promise((r) => setTimeout(r, 0));

    expect(screen.queryByText('Stale Result')).not.toBeInTheDocument();
    expect(screen.getByText('Newer Result')).toBeInTheDocument();
  });

  it('resets filters when a new search is started', async () => {
    const user = userEvent.setup();
    mockedGetPotentialSuppliers.mockResolvedValueOnce(
      response([supplier({ duns: 700000001, name: 'Old Search Result', country: 'ES' })]),
    );
    render(<Dashboard />);
    await runSearch(user, 500);
    await screen.findByText('Old Search Result');

    await user.type(screen.getByLabelText(/search/i), 'Old Search Result');
    expect(screen.getByText('Old Search Result')).toBeInTheDocument();

    // A brand-new search for a different amount returns results that don't match the leftover
    // filter text at all - if the filter weren't reset, this would render a false empty state
    // even though the server returned a non-empty result.
    mockedGetPotentialSuppliers.mockResolvedValueOnce(
      response([supplier({ duns: 700000002, name: 'Brand New Supplier', country: 'FR' })]),
    );
    await runSearch(user, 10000);

    expect(await screen.findByText('Brand New Supplier')).toBeInTheDocument();
    expect(screen.queryByText(/no suppliers match/i)).not.toBeInTheDocument();
    expect(screen.getByLabelText(/search/i)).toHaveValue('');
  });
});
