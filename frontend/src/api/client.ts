import type { ApiError } from '../types/supplier';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export class ApiClientError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
  }
}

export async function apiGet<T>(path: string, params?: Record<string, string | number>): Promise<T> {
  const url = new URL(path, BASE_URL || window.location.origin);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      url.searchParams.set(key, String(value));
    }
  }

  let response: Response;
  try {
    response = await fetch(url.toString());
  } catch {
    throw new ApiClientError('Unable to reach the server. Please check your connection and try again.');
  }

  if (!response.ok) {
    let info = `Request failed with status ${response.status}`;
    try {
      const body = (await response.json()) as ApiError;
      if (body?.info) {
        info = body.info;
      }
    } catch {
      // response body wasn't valid JSON — fall back to the generic message above.
    }
    throw new ApiClientError(info, response.status);
  }

  return (await response.json()) as T;
}
