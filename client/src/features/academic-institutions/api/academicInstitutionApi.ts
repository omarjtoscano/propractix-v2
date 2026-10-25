import { academicInstitutionSearchConfiguration as configuration } from '../../../configuration/academicInstitutionSearch';
import { createClient, createConfig } from '../../../shared/api/generated/client';
import { searchAcademicInstitutions } from '../../../shared/api/generated/sdk.gen';
import type { AcademicInstitutionSearchPage } from '../../../shared/api/generated';

export class AcademicInstitutionApiError extends Error {
  constructor(readonly code: string, readonly status = 0, readonly retryAt = 0) {
    super(code);
  }
}

/** Retry-After is scoped to the operation, including new queries and component remounts. */
export class AcademicInstitutionApi {
  private retryAt = 0;
  private retryCode = 'rate_limited';
  private readonly listeners = new Set<() => void>();
  private readonly client = createClient(createConfig({
    baseUrl: globalThis.location?.origin,
    fetch: (request) => globalThis.fetch(request),
  }));

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => { this.listeners.delete(listener); };
  };
  getRetryAt = () => this.retryAt;

  async search(query: string, cursor: string | null, signal: AbortSignal): Promise<AcademicInstitutionSearchPage> {
    if (Date.now() < this.retryAt) {
      throw new AcademicInstitutionApiError(this.retryCode, 429, this.retryAt);
    }
    try {
      const result = await searchAcademicInstitutions({
        client: this.client,
        query: { query, limit: configuration.pageSize, ...(cursor === null ? {} : { cursor }) },
        signal,
        throwOnError: false,
      });
      // A transport that ignores AbortSignal must still not publish an obsolete response.
      signal.throwIfAborted();
      if (result.error || !result.data) {
        const status = result.response?.status ?? 0;
        const code = typeof result.error?.code === 'string' ? result.error.code : 'unexpected_error';
        if (status === 429 || status === 503) {
          this.retryAt = Math.max(this.retryAt, retryAfterDeadline(result.response?.headers.get('Retry-After') ?? null));
          this.retryCode = code;
          this.listeners.forEach((listener) => listener());
        }
        throw new AcademicInstitutionApiError(code, status, this.retryAt);
      }
      return validatePage(result.data);
    } catch (error) {
      if (signal.aborted || error instanceof AcademicInstitutionApiError) throw error;
      throw new AcademicInstitutionApiError('unexpected_error');
    }
  }
}

export function retryAfterDeadline(value: string | null, now = Date.now()): number {
  if (value !== null && /^\d+$/u.test(value)) {
    const seconds = Number(value);
    if (Number.isSafeInteger(seconds) && seconds > 0 && seconds <= (Number.MAX_SAFE_INTEGER - now) / 1000) {
      return now + seconds * 1000;
    }
  }
  // Also accept the standard HTTP-date form; malformed/missing headers use a conservative delay.
  const date = value !== null && /^[A-Za-z]{3}, /u.test(value) ? Date.parse(value) : Number.NaN;
  return Number.isFinite(date) && date > now ? date : now + configuration.retryFallbackMs;
}

export function sameSnapshot(left: AcademicInstitutionSearchPage['snapshot'], right: AcademicInstitutionSearchPage['snapshot']) {
  return left.retrievedAt === right.retrievedAt && left.sourceUrl === right.sourceUrl
    && left.sourceAttribution === right.sourceAttribution;
}

function validatePage(page: AcademicInstitutionSearchPage): AcademicInstitutionSearchPage {
  // The generated client owns transport types; validate public data before creating links or ARIA IDs.
  const snapshot = page?.snapshot;
  let source: URL;
  try { source = new URL(snapshot?.sourceUrl); } catch { throw new AcademicInstitutionApiError('unexpected_error'); }
  if (!['http:', 'https:'].includes(source.protocol) || source.username || source.password
    || !Number.isFinite(Date.parse(snapshot.retrievedAt)) || typeof snapshot.sourceAttribution !== 'string'
    || !Array.isArray(page.items) || page.items.length > 50
    || !(page.nextCursor === null || typeof page.nextCursor === 'string' && /^[A-Za-z0-9_-]{1,4096}$/u.test(page.nextCursor))
    || !page.items.every((item) => typeof item.officialName === 'string' && item.officialName.length > 0
      && typeof item.id === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iu.test(item.id))) {
    throw new AcademicInstitutionApiError('unexpected_error');
  }
  return page;
}

export const academicInstitutionApi = new AcademicInstitutionApi();
