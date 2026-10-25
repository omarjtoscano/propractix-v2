import { academicInstitutionSearchConfiguration as config } from '../../../configuration/academicInstitutionSearch';
import { AcademicInstitutionApi, retryAfterDeadline } from './academicInstitutionApi';

const item = { id: '11111111-1111-4111-8111-111111111111', officialName: 'Universidad Sintética', countryCode: 'ES' };
const page = { items: [item], nextCursor: null, snapshot: {
  retrievedAt: '2026-09-21T12:00:00Z', sourceUrl: 'https://example.test/catalog', sourceAttribution: 'Synthetic fixture',
} };
const signal = () => new AbortController().signal;

function response(body: unknown, status = 200, headers = {}) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json', ...headers } });
}
afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });

it('uses the generated GET operation with encoded query, opaque cursor and configured limit', async () => {
  const fetch = vi.fn().mockResolvedValue(response(page));
  vi.stubGlobal('fetch', fetch);
  const result = await new AcademicInstitutionApi().search('Á & Norte', 'opaque_cursor', signal());
  const request = fetch.mock.calls[0][0] as Request;
  const url = new URL(request.url);
  expect(request.method).toBe('GET');
  expect(url.pathname).toBe('/api/v1/academic-institutions');
  expect(url.searchParams.get('query')).toBe('Á & Norte');
  expect(url.searchParams.get('limit')).toBe(String(config.pageSize));
  expect(url.searchParams.get('cursor')).toBe('opaque_cursor');
  expect(result.items[0].id).toBe(item.id);
});

it.each([
  [400, 'validation_error'], [409, 'catalog_snapshot_changed'], [429, 'rate_limited'],
  [503, 'rate_limiter_unavailable'], [503, 'academic_institution_catalog_unavailable'],
])('preserves public codes from HTTP %s without exposing diagnostic prose', async (status, code) => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response({ code, title: 'PRIVATE', detail: 'SECRET' }, status)));
  await expect(new AcademicInstitutionApi().search('', null, signal())).rejects.toMatchObject({ code, status });
});

it('enforces Retry-After across different queries and cursors without a network call', async () => {
  vi.useFakeTimers();
  const fetch = vi.fn().mockResolvedValueOnce(response({ code: 'rate_limited' }, 429, { 'Retry-After': '5' }))
    .mockResolvedValue(response(page));
  vi.stubGlobal('fetch', fetch);
  const api = new AcademicInstitutionApi();
  await expect(api.search('first', null, signal())).rejects.toMatchObject({ code: 'rate_limited' });
  await expect(api.search('other', 'next', signal())).rejects.toMatchObject({ code: 'rate_limited' });
  await vi.advanceTimersByTimeAsync(4999);
  await expect(api.search('', null, signal())).rejects.toMatchObject({ code: 'rate_limited' });
  expect(fetch).toHaveBeenCalledTimes(1);
  await vi.advanceTimersByTimeAsync(1);
  await expect(api.search('other', null, signal())).resolves.toMatchObject(page);
  expect(fetch).toHaveBeenCalledTimes(2);
});

it.each([null, '', '0', '-1', 'broken', '99999999999999999999999999999999999'])('uses the configured fallback for malformed Retry-After %s', (value) => {
  expect(retryAfterDeadline(value, 0)).toBe(config.retryFallbackMs);
});

it('accepts an HTTP-date Retry-After', () => {
  const now = Date.parse('2026-10-25T12:00:00Z');
  expect(retryAfterDeadline('Sun, 25 Oct 2026 12:00:10 GMT', now)).toBe(now + 10_000);
});

it('discards a response when transport ignores cancellation', async () => {
  const controller = new AbortController();
  vi.stubGlobal('fetch', vi.fn(async () => { controller.abort(); return response(page); }));
  await expect(new AcademicInstitutionApi().search('', null, controller.signal)).rejects.toBeDefined();
});

it.each(['javascript:alert(1)', 'https://user:password@example.test', 'data:text/html,secret'])('rejects an unsafe snapshot link %s', async (sourceUrl) => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response({ ...page, snapshot: { ...page.snapshot, sourceUrl } })));
  await expect(new AcademicInstitutionApi().search('', null, signal())).rejects.toMatchObject({ code: 'unexpected_error' });
});

it('maps a network exception to a safe code', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('PRIVATE NETWORK DETAILS')));
  await expect(new AcademicInstitutionApi().search('', null, signal())).rejects.toMatchObject({ code: 'unexpected_error' });
});
