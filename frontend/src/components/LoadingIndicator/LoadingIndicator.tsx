export function LoadingIndicator() {
  return (
    <div className="loading-indicator" role="status" aria-live="polite">
      <span className="loading-indicator__spinner" aria-hidden="true" />
      Loading suppliers…
    </div>
  );
}
