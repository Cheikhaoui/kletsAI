import { HttpErrorResponse } from '@angular/common/http';

/** A short message for the user from a failed API call (uses the ProblemDetail "detail" when present). */
export function errorMessage(e: unknown, fallback: string): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) {
      return 'No connection. Check your internet and try again.';
    }
    if (typeof e.error?.detail === 'string') {
      return e.error.detail;
    }
  }
  return fallback;
}
