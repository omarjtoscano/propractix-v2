import { readFile } from 'node:fs/promises';

const locales = ['es', 'en'];
const flatten = (value, prefix = '') => Object.entries(value).flatMap(([key, child]) => {
  const path = prefix ? `${prefix}.${key}` : key;
  return typeof child === 'object' && child !== null ? flatten(child, path) : [path];
});

const catalogs = await Promise.all(locales.map(async (locale) => {
  const content = await readFile(new URL(`../src/shared/i18n/locales/${locale}.json`, import.meta.url));
  return [locale, new Set(flatten(JSON.parse(content.toString())))];
}));

const [referenceLocale, reference] = catalogs[0];
for (const [locale, keys] of catalogs.slice(1)) {
  const missing = [...reference].filter((key) => !keys.has(key));
  const extra = [...keys].filter((key) => !reference.has(key));
  if (missing.length > 0 || extra.length > 0) {
    throw new Error(`i18n_key_mismatch:${referenceLocale}:${locale}:missing=${missing}:extra=${extra}`);
  }
}
