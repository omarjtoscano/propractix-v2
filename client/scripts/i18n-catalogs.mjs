import { readFile } from 'node:fs/promises';

export function flattenCatalog(value, prefix = '') {
  if (typeof value === 'string' && value.trim() !== '') return [[prefix, value]];
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`invalid_i18n_translation:${prefix}`);
  }
  const entries = Object.entries(value);
  if (entries.length === 0) throw new Error(`empty_i18n_catalog:${prefix}`);
  return entries.flatMap(([key, child]) => flattenCatalog(child, prefix ? `${prefix}.${key}` : key));
}

export function compareCatalogs(reference, candidate) {
  const referenceEntries = new Map(flattenCatalog(reference));
  const candidateEntries = new Map(flattenCatalog(candidate));
  const issues = [];
  const parameters = (text) => [...text.matchAll(/\{\{-?\s*([^},\s]+)[^}]*\}\}/gu)]
    .map((match) => match[1]).sort().join(',');
  for (const [key, text] of referenceEntries) {
    if (!candidateEntries.has(key)) issues.push(`missing:${key}`);
    else if (parameters(text) !== parameters(candidateEntries.get(key))) issues.push(`parameters:${key}`);
  }
  for (const key of candidateEntries.keys()) {
    if (!referenceEntries.has(key)) issues.push(`extra:${key}`);
  }
  return issues;
}

export async function readCatalogs() {
  const config = JSON.parse(await readFile(
    new URL('../src/configuration/product-catalog.json', import.meta.url), 'utf8',
  ));
  if (!config.supportedLocales.includes(config.fallbackLocale)) {
    throw new Error('invalid_i18n_fallback_configuration');
  }
  const catalogs = new Map();
  for (const locale of config.supportedLocales) {
    if (!/^[a-z]{2}(?:-[A-Z]{2})?$/u.test(locale)) throw new Error('invalid_i18n_locale');
    const catalog = JSON.parse(await readFile(
      new URL(`../src/shared/i18n/locales/${locale}.json`, import.meta.url), 'utf8',
    ));
    flattenCatalog(catalog);
    catalogs.set(locale, catalog);
  }
  return { catalogs, fallbackLocale: config.fallbackLocale };
}
