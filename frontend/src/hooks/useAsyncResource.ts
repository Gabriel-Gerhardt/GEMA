import { useCallback, useEffect, useRef, useState } from 'react';

interface AsyncResource<T> {
  data: T | null;
  isLoading: boolean;
  /** Ready-to-render message. */
  error: string | null;
  /** The thrown value, so a caller can branch on an ApiError's status. */
  errorCause: unknown;
  reload: () => void;
}

/**
 * Loads something from the API and tracks the three states every screen needs
 * to show: loading, failed, loaded.
 *
 * Ignores a response that arrives after the screen is gone, so navigating away
 * mid-request doesn't set state on an unmounted component.
 */
export function useAsyncResource<T>(load: () => Promise<T>, deps: unknown[] = []): AsyncResource<T> {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [errorCause, setErrorCause] = useState<unknown>(null);
  const [attempt, setAttempt] = useState(0);
  const alive = useRef(true);

  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setError(null);
    setErrorCause(null);

    load()
      .then((result) => {
        if (!cancelled && alive.current) setData(result);
      })
      .catch((e: unknown) => {
        if (!cancelled && alive.current) {
          setError(e instanceof Error ? e.message : 'Algo deu errado.');
          setErrorCause(e);
        }
      })
      .finally(() => {
        if (!cancelled && alive.current) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);

  return { data, isLoading, error, errorCause, reload };
}
