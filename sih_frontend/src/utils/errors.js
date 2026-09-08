/**
 * Turns an axios error into a specific, human-readable message instead of
 * always showing the same generic "network error" line regardless of cause.
 */
export function getErrorMessage(err, fallback = 'Something went wrong. Please try again.') {
  if (!err) return fallback;

  // Request went out but got no response at all: backend down, wrong port, or CORS-blocked
  if (err.request && !err.response) {
    return "Can't reach the server. Make sure the backend is running and reachable at " +
      (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api') + '.';
  }

  const status = err.response?.status;
  const backendMessage = err.response?.data?.message || err.response?.data?.error;

  if (status === 401) return 'Your session expired. Please log in again.';
  if (status === 403) return "You don't have permission to do that.";
  if (status === 404) return backendMessage || 'Not found.';
  if (status === 400) return backendMessage || 'That request was invalid. Check the details and try again.';
  if (status >= 500) return `Server error${backendMessage ? `: ${backendMessage}` : ' — check the backend console for the stack trace.'}`;

  return backendMessage || fallback;
}

/**
 * Same as getErrorMessage, but for requests made with responseType: 'blob'
 * (file downloads). Axios still puts the error JSON in err.response.data, but
 * as a Blob instead of a parsed object, so the normal sync path can't read it.
 * This reads the blob text first and falls back to getErrorMessage otherwise.
 */
export async function getErrorMessageFromBlob(err, fallback = 'Something went wrong. Please try again.') {
  if (err?.response?.data instanceof Blob) {
    try {
      const text = await err.response.data.text();
      const parsed = JSON.parse(text);
      const backendMessage = parsed.message || parsed.error;
      const status = err.response.status;
      if (status === 400) return backendMessage || 'That request was invalid. Check the details and try again.';
      if (status >= 500) return `Server error${backendMessage ? `: ${backendMessage}` : ''}`;
      return backendMessage || fallback;
    } catch {
      // Body wasn't JSON (e.g. actually was a valid PDF, or empty) - fall through
    }
  }
  return getErrorMessage(err, fallback);
}
