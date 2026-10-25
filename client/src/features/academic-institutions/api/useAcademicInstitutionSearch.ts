import { useEffect, useId, useRef, useState, useSyncExternalStore } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import { academicInstitutionSearchConfiguration as configuration } from '../../../configuration/academicInstitutionSearch';
import { AcademicInstitutionApiError, academicInstitutionApi as api, sameSnapshot } from './academicInstitutionApi';

export function useAcademicInstitutionSearch(text: string, active: boolean) {
  const scope = useId();
  const [debounced, setDebounced] = useState<{ text: string; active: boolean } | null>(null);
  const [generation, setGeneration] = useState(0);
  const [snapshotChanged, setSnapshotChanged] = useState(false);
  const restarted = useRef(false);
  const retryAt = useSyncExternalStore(api.subscribe, api.getRetryAt, api.getRetryAt);
  const [now, setNow] = useState(Date.now);
  const ready = active && debounced?.active === active && debounced.text === text;

  useEffect(() => {
    setDebounced(null);
    restarted.current = false;
    setSnapshotChanged(false);
    if (!active) return;
    const timeout = setTimeout(() => setDebounced({ text, active }), configuration.debounceMs);
    return () => clearTimeout(timeout);
  }, [text, active]);

  useEffect(() => {
    setNow(Date.now());
    if (retryAt <= Date.now()) return;
    const interval = setInterval(() => {
      setNow(Date.now());
      if (Date.now() >= retryAt) clearInterval(interval);
    }, 250);
    return () => clearInterval(interval);
  }, [retryAt]);

  const query = useInfiniteQuery({
    queryKey: ['academic-institutions', scope, text, active, generation],
    initialPageParam: null as string | null,
    queryFn: ({ pageParam, signal }) => api.search(text, pageParam, signal),
    getNextPageParam: (lastPage) => lastPage.nextCursor,
    enabled: ready,
    staleTime: Infinity,
    gcTime: 0,
    retry: false,
    retryOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
  const pages = ready ? query.data?.pages ?? [] : [];
  const mixedSnapshots = pages.some((page) => !sameSnapshot(pages[0].snapshot, page.snapshot));
  const error = ready ? mixedSnapshots
    ? new AcademicInstitutionApiError('catalog_snapshot_changed', 409)
    : query.error instanceof AcademicInstitutionApiError ? query.error : query.error
      ? new AcademicInstitutionApiError('unexpected_error') : null : null;
  const changed = error?.code === 'catalog_snapshot_changed';

  useEffect(() => {
    // At most one automatic restart per search; repeated conflicts require explicit retry.
    if (changed && !restarted.current) {
      restarted.current = true;
      setSnapshotChanged(true);
      setGeneration((value) => value + 1);
    }
  }, [changed]);

  const visiblePages = error ? [] : pages;
  const items = [...new Map(visiblePages.flatMap((page) => page.items).map((item) => [item.id, item])).values()];
  const waitSeconds = Math.max(0, Math.ceil((retryAt - now) / 1000));

  return {
    items,
    snapshot: visiblePages[0]?.snapshot,
    snapshotChanged,
    error,
    loading: active && (!ready || query.isFetching),
    hasNextPage: ready && !error && query.hasNextPage,
    waitSeconds,
    loadMore: () => {
      if (ready && !query.isFetching && !error && query.hasNextPage && Date.now() >= retryAt) {
        void query.fetchNextPage({ cancelRefetch: false });
      }
    },
    retry: () => {
      if (Date.now() < retryAt || query.isFetching) return;
      // Reset pages and cursor together; do not refetch a stale chain of cursors.
      restarted.current = false;
      setGeneration((value) => value + 1);
    },
  };
}
