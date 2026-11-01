import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { readFileSync, writeFileSync } from 'node:fs';
import { createHmac } from 'node:crypto';
import { compose, metric, publish, root, sql } from '../../scripts/e2e-environment.mjs';
import type { AcademicInstitutionSearchPage } from '../src/shared/api/generated';
import es from '../src/shared/i18n/locales/es.json' with { type: 'json' };
import en from '../src/shared/i18n/locales/en.json' with { type: 'json' };

const endpoint = '/api/v1/academic-institutions';
const uuidV4 = /[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}/;
const sensitiveQuery = 'E8-PRIVATE-QUERY-CANARY';
const sensitiveAgent = 'E8-PRIVATE-AGENT-CANARY';
const manualName = 'E8-PRIVATE-MANUAL-CANARY';

async function open(page: Page, locale: 'es' | 'en' = 'es') {
  await page.addInitScript((value) => localStorage.setItem('propractix.ui.locale', value), locale);
  await page.goto('/');
  await expect(page.locator('html')).toHaveAttribute('lang', locale);
}

async function search(page: Page, term: string, label = es.academicInstitutions.label) {
  const response = page.waitForResponse((value) => value.url().includes(endpoint) && value.request().method() === 'GET');
  await page.getByRole('combobox', { name: label, exact: true }).fill(term);
  return await response;
}

function catalogState() {
  return sql(`SELECT json_build_object(
    'institutions', (SELECT count(*) FROM academicinstitution_institution),
    'imports', (SELECT count(*) FROM academicinstitution_catalog_import),
    'tables', (SELECT array_agg(tablename ORDER BY tablename) FROM pg_tables WHERE schemaname='public'))`);
}

async function alignBurst() {
  // Only wait near a real PostgreSQL boundary; never change production clocks/limits to pass a test.
  await expect.poll(() => Number(sql('SELECT 10 - mod(extract(epoch FROM CURRENT_TIMESTAMP)::numeric, 10)')),
    { intervals: [100], timeout: 4000 }).toBeGreaterThan(3);
}

async function fillBurst(request: APIRequestContext, spoof = false) {
  await alignBurst();
  for (let index = 0; index < 15; index++) {
    const response = await request.get(`${endpoint}?query=${sensitiveQuery}-${index}`, { headers: {
      'User-Agent': sensitiveAgent,
      ...(spoof ? { 'X-Forwarded-For': `198.51.100.${index + 1}`, Forwarded: `for=203.0.113.${index + 1}` } : {}),
    } });
    expect(response.status()).toBe(200);
  }
}

test.beforeEach(() => {
  // This helper is restricted to the disposable Compose project, never the developer database.
  sql('TRUNCATE platform_rate_limit_bucket');
});

for (const [locale, messages] of [['es', es], ['en', en]] as const) {
  test(`accent-insensitive search, UUID selection and keyboard navigation (${locale})`, async ({ page }) => {
    const before = catalogState();
    await open(page, locale);
    await page.keyboard.press('Tab');
    await expect(page.getByRole('combobox', { name: messages.locales.label })).toBeFocused();
    await page.keyboard.press('Tab');
    const input = page.getByRole('combobox', { name: messages.academicInstitutions.label, exact: true });
    await expect(input).toBeFocused();
    const firstResponse = await search(page, 'Álamo', messages.academicInstitutions.label);
    expect(firstResponse.status()).toBe(200);
    const first: AcademicInstitutionSearchPage = await firstResponse.json();
    const secondResponse = await search(page, 'alamo', messages.academicInstitutions.label);
    const second: AcademicInstitutionSearchPage = await secondResponse.json();
    expect(second.items).toEqual(first.items);
    expect(second.items).toHaveLength(20);
    const item = second.items[0];
    expect(item.id).toMatch(uuidV4);
    expect(item.id).not.toContain('E8-');
    const option = page.getByRole('option', { name: item.officialName, exact: true });
    await expect(option).toHaveAttribute('id', new RegExp(item.id));
    await input.press('ArrowDown');
    await expect(input).toHaveAttribute('aria-activedescendant', await option.getAttribute('id') as string);
    await input.press('Enter');
    await expect(input).toHaveValue(item.officialName);
    await expect(input).toBeFocused();
    await expect(input).toHaveAttribute('aria-expanded', 'false');
    await expect(page.getByText(messages.academicInstitutions.selected.replace('{{name}}', item.officialName), { exact: true })).toBeVisible();
    await expect(page.getByRole('complementary', { name: messages.academicInstitutions.attribution })).toBeVisible();
    // The language selector also works after a real catalog selection.
    const nextLocale = locale === 'es' ? 'en' : 'es';
    await page.getByRole('combobox', { name: messages.locales.label }).selectOption(nextLocale);
    await expect(page.locator('html')).toHaveAttribute('lang', nextLocale);
    expect(catalogState()).toEqual(before);
  });
}

test('pagination has no duplicates and a changed snapshot restarts from page one', async ({ page }) => {
  await open(page);
  const first = await search(page, 'Sintética');
  const firstPage: AcademicInstitutionSearchPage = await first.json();
  const ids = firstPage.items.map((item) => item.id);
  expect(ids).toHaveLength(20);
  const more = page.getByRole('button', { name: es.academicInstitutions.more });
  for (const expectedCount of [40, 45]) {
    await expect(more).toBeEnabled();
    const next = page.waitForResponse((r) => r.url().includes('cursor=') && r.status() === 200);
    if (expectedCount === 45) {
      await page.getByRole('combobox', { name: es.academicInstitutions.label, exact: true }).press('Tab');
      await expect(more, await page.evaluate(() => document.activeElement?.tagName)).toBeFocused();
      await page.keyboard.press('Enter');
    } else {
      await more.click();
    }
    const body: AcademicInstitutionSearchPage = await (await next).json();
    ids.push(...body.items.map((item) => item.id));
    await expect(page.getByRole('listbox').getByRole('option')).toHaveCount(expectedCount);
  }
  expect(new Set(ids).size).toBe(45);
  await expect(more).toHaveCount(0);
  const restartedFirst = await search(page, 'alamo');
  const old: AcademicInstitutionSearchPage = await restartedFirst.json();
  // Same accepted administrative flow, not direct catalog SQL writes.
  publish(2);
  const changed = page.waitForResponse((r) => r.url().includes('cursor=') && r.status() === 409);
  const restarted = page.waitForResponse((r) => r.url().includes(endpoint) && !r.url().includes('cursor=') && r.status() === 200);
  await more.click();
  expect((await (await changed).json()).code).toBe('catalog_snapshot_changed');
  const current: AcademicInstitutionSearchPage = await (await restarted).json();
  expect(current.snapshot.retrievedAt).not.toBe(old.snapshot.retrievedAt);
  expect(current.items.map((item) => item.id)).toEqual(old.items.map((item) => item.id));
  await expect(page.getByRole('listbox').getByRole('option')).toHaveCount(20);
  await expect(page.getByText(es.academicInstitutions.changed, { exact: true })).toBeVisible();
});

test('manual entry is local, makes no write requests and creates no institution or account', async ({ page }) => {
  const before = catalogState();
  const writes: string[] = [];
  page.on('request', (request) => { if (!['GET', 'HEAD'].includes(request.method())) writes.push(request.method()); });
  await open(page, 'en');
  await search(page, 'alamo', en.academicInstitutions.label);
  await page.getByRole('combobox', { name: en.academicInstitutions.label, exact: true }).press('Escape');
  await page.keyboard.press('Tab'); // next page control disappears when the combobox closes
  await page.keyboard.press('Tab'); // source link, then manual action
  const manual = page.getByRole('button', { name: en.academicInstitutions.manual });
  // Reach the manual action using keyboard only, regardless of attribution links.
  for (let index = 0; index < 3 && !(await manual.evaluate((element) => element === document.activeElement)); index++) {
    await page.keyboard.press('Tab');
  }
  await expect(manual).toBeFocused();
  await page.keyboard.press('Enter');
  const input = page.getByRole('textbox', { name: en.academicInstitutions.manualLabel });
  await expect(input).toBeFocused();
  await page.keyboard.type(manualName);
  await expect(page.getByText(en.academicInstitutions.selectedManual.replace('{{name}}', manualName), { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByText(manualName)).toHaveCount(0);
  expect(writes).toEqual([]);
  expect(catalogState()).toEqual(before);
  expect(sql("SELECT string_agg(tablename, ',' ORDER BY tablename) FROM pg_tables WHERE schemaname='public'"))
    .toBe('academicinstitution_catalog_import,academicinstitution_institution,flyway_schema_history,platform_rate_limit_bucket');
});

test('real 429 disables retries until Retry-After, then the browser recovers', async ({ page, request }) => {
  await open(page);
  await fillBurst(request);
  const limited = await search(page, sensitiveQuery);
  expect(limited.status()).toBe(429);
  const retryAfter = Number(limited.headers()['retry-after']);
  expect(retryAfter).toBeGreaterThan(0);
  expect(retryAfter).toBeLessThanOrEqual(10);
  const receivedAt = Date.now();
  const retry = page.getByRole('button', { name: es.academicInstitutions.retry });
  await expect(page.getByRole('alert')).toHaveText(es.errors.rate_limited);
  await expect(retry).toBeDisabled();
  let requests = 0;
  page.on('request', (r) => { if (r.url().includes(endpoint)) requests++; });
  await page.getByRole('combobox', { name: es.academicInstitutions.label, exact: true }).fill('alamo');
  await page.waitForTimeout(350); // crosses the debounce while Retry-After is still active
  expect(requests).toBe(0);
  await expect(retry).toBeEnabled({ timeout: (retryAfter + 2) * 1000 });
  const recovered = page.waitForResponse((r) => r.url().includes(endpoint) && r.status() === 200);
  await retry.click();
  expect((await recovered).status()).toBe(200);
  expect(Date.now() - receivedAt).toBeGreaterThanOrEqual(retryAfter * 1000 - 250);
  await expect(page.getByRole('listbox').getByRole('option')).toHaveCount(20);
});

test('changing forged forwarding headers cannot create another quota through Caddy', async ({ request }) => {
  await fillBurst(request, true);
  const limited = await request.get(endpoint, { headers: { 'X-Forwarded-For': '192.0.2.99', Forwarded: 'for=192.0.2.100' } });
  expect(limited.status()).toBe(429);
  expect((await limited.json()).code).toBe('rate_limited');
  // Only aggregates leave storage; never select/export fingerprints or key versions.
  expect(sql("SELECT count(*) FROM platform_rate_limit_bucket WHERE rule_id='burst' AND expires_at > CURRENT_TIMESTAMP")).toBe('1');
  expect(Number(sql("SELECT max(request_count) FROM platform_rate_limit_bucket WHERE rule_id='burst'"))).toBe(15);
});

test('unavailable query and rate-limit adapters fail closed with translated 503s and recover', async ({ page, request }) => {
  await open(page, 'en');
  sql('ALTER TABLE academicinstitution_institution RENAME TO e8_unavailable_institution');
  try {
    const response = await search(page, sensitiveQuery, en.academicInstitutions.label);
    expect(response.status()).toBe(503);
    expect((await response.json()).code).toBe('academic_institution_catalog_unavailable');
    await expect(page.getByRole('alert')).toHaveText(en.errors.academic_institution_catalog_unavailable);
    await expect(page.getByRole('listbox').getByRole('option')).toHaveCount(0);
  } finally {
    sql('ALTER TABLE e8_unavailable_institution RENAME TO academicinstitution_institution');
  }
  const recovered = page.waitForResponse((r) => r.url().includes(endpoint) && r.status() === 200);
  await page.getByRole('button', { name: en.academicInstitutions.retry }).click();
  await recovered;
  sql('ALTER TABLE platform_rate_limit_bucket RENAME TO e8_unavailable_bucket');
  try {
    const response = await request.get(`${endpoint}?query=${sensitiveQuery}`);
    expect(response.status()).toBe(503);
    expect(response.headers()['retry-after']).toBe('1');
    expect((await response.json()).code).toBe('rate_limiter_unavailable');
  } finally {
    sql('ALTER TABLE e8_unavailable_bucket RENAME TO platform_rate_limit_bucket');
  }
  expect((await request.get(endpoint)).status()).toBe(200);
});

test('private metrics, publication signals and logs contain only bounded operational data', async ({ page, request }) => {
  publish(2);
  const invalid = publish(2, { invalid: true });
  expect(invalid.stdout).toContain('code=catalog_artifact_hash_mismatch');
  expect(invalid.stderr).toContain('"event":"catalog_publication","outcome":"failure"');
  const repeated = publish(2);
  expect(repeated.stdout).toContain('code=catalog_active_artifact_unchanged');
  expect(repeated.stderr).toContain('"event":"catalog_publication","outcome":"success"');
  await open(page);
  await search(page, sensitiveQuery);
  const cursorPage: AcademicInstitutionSearchPage = await (await request.get(endpoint)).json();
  expect(cursorPage.nextCursor).toBeTruthy();
  expect((await request.get(endpoint, { params: { cursor: cursorPage.nextCursor! } })).status()).toBe(200);
  // Generate these signals here too, so this test is valid when selected with --grep.
  sql('TRUNCATE platform_rate_limit_bucket');
  await fillBurst(request);
  expect((await request.get(endpoint)).status()).toBe(429);
  sql('ALTER TABLE platform_rate_limit_bucket RENAME TO e8_unavailable_bucket');
  try {
    expect((await request.get(endpoint)).status()).toBe(503);
  } finally {
    sql('ALTER TABLE e8_unavailable_bucket RENAME TO platform_rate_limit_bucket');
  }
  // No metrics/admin HTTP access, through Caddy or the public API.
  expect((await request.get('/api/v1/metrics')).status()).toBe(403);
  const names = ['propractix_rate_limit_allowed_total', 'propractix_rate_limit_rejected_total',
    'propractix_rate_limit_adapter_errors_total', 'propractix_catalog_search_duration', 'propractix_catalog_snapshot_age_seconds'];
  const metrics = names.map((name) => metric(name));
  for (let index = 0; index < names.length; index++) expect(metrics[index]).toContain(names[index]);
  expect(metrics[0]).toContain('searchAcademicInstitutions');
  expect(metrics[1]).toContain('burst');
  expect(metrics[2]).toContain('storage');
  expect(metrics[3]).toContain('unavailable');
  expect(metrics[4]).not.toContain('value=NaN');
  for (const value of metrics) {
    expect(Number(value.match(/value=([0-9.]+)/)?.[1])).toBeGreaterThan(0);
  }
  const logs = compose(['logs', '--no-color']).stdout;
  const publications = readFileSync(`${root}/.local/e2e/publications.log`, 'utf8');
  const signals = logs + publications + metrics.join('\n');
  const hmac = readFileSync(`${root}/.local/e2e/secrets/rate-limit-hmac-rl-ip-v1`, 'utf8');
  const addresses = [process.env.PROPRACTIX_E2E_CLIENT_GATEWAY!, process.env.PROPRACTIX_E2E_PROXY_PEER!,
    '198.51.100.1', '203.0.113.1', '192.0.2.99', '192.0.2.100'];
  const canonical = addresses.map((address) => `ipv4:${Buffer.from(address.split('.').map(Number)).toString('hex')}/32`);
  const fingerprints = canonical.map((network) => createHmac('sha256', hmac)
    .update('propractix:ratelimit-ip:v1\n').update(network).digest('base64url'));
  for (const forbidden of [sensitiveQuery, sensitiveAgent, manualName, cursorPage.nextCursor!, hmac, '198.51.100.', '203.0.113.',
    '192.0.2.99', '192.0.2.100', 'remote_ip', '"client_ip":', 'subject_fingerprint', 'user_agent', 'rl-ip-v1',
    ...addresses, ...canonical, ...fingerprints]) {
    expect(signals.includes(forbidden), 'Telemetry retained a private canary').toBe(false);
  }
  expect(sql("SELECT count(*) FROM academicinstitution_institution WHERE source_system <> 'SYNTHETIC'")).toBe('0');
  writeFileSync(`${root}/.local/e2e/metrics.json`, JSON.stringify(names.map((name, index) => ({
    name, firstMeasurement: Number(metrics[index].match(/value=([0-9.]+)/)?.[1]),
  })), null, 2));
  // Defaults prohibit browser artifacts that would retain requests or manual values.
  expect(readFileSync(`${root}/client/playwright.config.ts`, 'utf8')).toContain("trace: 'off'");
});
