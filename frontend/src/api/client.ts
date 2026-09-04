// Thin fetch wrapper. TODO: implement.
//
// Responsibilities to cover here (not in individual API modules or components):
// - Resolve the base URL from import.meta.env.VITE_API_BASE_URL.
// - Parse non-2xx responses into ApiError (schema: { info: string }) and throw a typed error.
// - Surface network failures (fetch rejecting) distinctly from HTTP error responses, so
//   components can show the same "friendly error message" either way (README frontend
//   requirements table, "Error handling").

export class ApiClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
  }
}

export async function apiGet<T>(_path: string, _params?: Record<string, string | number>): Promise<T> {
  throw new Error('TODO: implement apiGet');
}
