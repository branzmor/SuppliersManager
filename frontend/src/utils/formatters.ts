const currencyFormatter = new Intl.NumberFormat('es-ES', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
});

const scoreFormatter = new Intl.NumberFormat('es-ES', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

export function formatCurrencyEur(amountInEuros: number): string {
  return currencyFormatter.format(amountInEuros);
}

export function formatScore(score: number): string {
  return scoreFormatter.format(score);
}
