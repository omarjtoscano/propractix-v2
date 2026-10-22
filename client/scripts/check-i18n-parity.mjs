import { compareCatalogs, readCatalogs } from './i18n-catalogs.mjs';

const { catalogs, fallbackLocale } = await readCatalogs();
for (const [locale, catalog] of catalogs) {
  const issues = compareCatalogs(catalogs.get(fallbackLocale), catalog);
  if (issues.length > 0) throw new Error(`i18n_catalog_mismatch:${locale}:${issues.join(',')}`);
}
