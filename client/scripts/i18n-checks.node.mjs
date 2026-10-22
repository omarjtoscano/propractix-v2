import assert from 'node:assert/strict';
import { test } from 'node:test';
import { compareCatalogs, flattenCatalog } from './i18n-catalogs.mjs';
import { visibleTextViolations } from './visible-text.mjs';

const keys = new Set(['errors.unexpected_error', 'actions.retry']);
const check = (source) => visibleTextViolations(source, 'fixture.tsx', keys);

test('rejects visible literals in text, accessible attributes and JSX expressions', () => {
  const examples = [
    '<p>Error inesperado</p>',
    '<input placeholder="Your email" />',
    '<button aria-label={"Try again"} />',
    '<img alt="Company logo" />',
    '<p>{"An error occurred"}</p>',
    '<p>{failed ? "Error" : "Ready"}</p>',
    '<p>{failed && "Error"}</p>',
    '<p>{`Error: ${code}`}</p>',
    '<p title={"Error " + code} />',
  ];
  for (const jsx of examples) {
    assert.ok(check(`const View = () => (${jsx});`).some((issue) => issue.startsWith('visible_text_')), jsx);
  }
});

test('rejects exception and Problem Details prose, including optional and indexed access', () => {
  for (const expression of ['error.message', 'problem.detail', 'problem.title', 'err?.message', 'failure["message"]']) {
    assert.ok(check(`const View = () => <p>{${expression}}</p>;`)
      .some((issue) => issue.startsWith('raw_error_text_')), expression);
  }
});

test('checks literal translation keys and rejects inline default text', () => {
  assert.ok(check('const text = t("missing.key");').some((issue) => issue.startsWith('unknown_i18n_key')));
  assert.ok(check('const text = t("actions.retry", "Retry");')
    .some((issue) => issue.startsWith('i18n_default_text_')));
  assert.ok(check('const text = t("actions.retry", { defaultValue: "Retry" });')
    .some((issue) => issue.startsWith('i18n_default_text_')));
});

test('allows translation calls, technical attributes, comments and source data', () => {
  assert.deepEqual(check(`
    // Developer diagnostic: Error.
    const View = () => <section role="alert" data-testid="error">
      <button type="button" aria-label={t('actions.retry')}>{t('actions.retry')}</button>
      <p>{institution.officialName}</p>
      <p>{t(errorTranslationKey(code))}</p>
    </section>;
    throw new Error('technical_diagnostic');
  `), []);
});

test('fails on unparsable source instead of silently skipping checks', () => {
  assert.throws(() => check('const View = () => <p>{;'), /i18n_source_parse_failed/u);
});

test('catalog parity includes nonempty strings and matching interpolation parameters', () => {
  const source = { errors: { retry: 'Retry in {{seconds}} seconds' } };
  assert.deepEqual(compareCatalogs(source, { errors: { retry: 'Reintenta en {{seconds}} segundos' } }), []);
  assert.deepEqual(compareCatalogs(source, { errors: { retry: 'Reintenta en {{minutes}} minutos' } }),
    ['parameters:errors.retry']);
  assert.deepEqual(compareCatalogs(source, { errors: { other: 'Otro' } }),
    ['missing:errors.retry', 'extra:errors.other']);
  for (const value of ['', '  ', null, 42, [], {}]) {
    assert.throws(() => flattenCatalog({ errors: { retry: value } }), /i18n/u);
  }
});
