// TODO: implement using Intl.NumberFormat('es-ES' or similar, { style: 'currency', currency: 'EUR' })
// for the "Annual Turnover" column (README frontend requirements: thousand separators + € symbol).
export function formatCurrencyEur(_amountInEuros: number): string {
  throw new Error('TODO: implement formatCurrencyEur');
}

// TODO: implement using toFixed(2) or Intl.NumberFormat with minimumFractionDigits/maximumFractionDigits: 2
// for the "Score" column (README: "Score (2 decimal places)").
export function formatScore(_score: number): string {
  throw new Error('TODO: implement formatScore');
}
