export type EmptyStateReason = 'no-results' | 'filtered-out';

const MESSAGES: Record<EmptyStateReason, string> = {
  'no-results': 'No potential suppliers found for this order amount.',
  'filtered-out': 'No suppliers on this page match the selected filters.',
};

export interface EmptyStateProps {
  /** Which empty case this is: the backend returned zero suppliers, or client-side filters hid all of them. */
  reason: EmptyStateReason;
}

export function EmptyState({ reason }: EmptyStateProps) {
  return (
    <div className="empty-state" role="status" aria-live="polite">
      {MESSAGES[reason]}
    </div>
  );
}
