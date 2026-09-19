import rawProductCatalog from './product-catalog.json';

export type ProductCatalog = Readonly<{
  enabledCountries: readonly string[];
  supportedLocales: readonly string[];
  countryDefaultLocales: Readonly<Record<string, string>>;
  fallbackLocale: string;
}>;

const countryPattern = /^[A-Z]{2}$/u;
const localePattern = /^[a-z]{2}(?:-[A-Z]{2})?$/u;

function validateProductCatalog(candidate: ProductCatalog): ProductCatalog {
  const countries = new Set(candidate.enabledCountries);
  const locales = new Set(candidate.supportedLocales);
  const mappedCountries = Object.keys(candidate.countryDefaultLocales);

  const valid = countries.size > 0
    && locales.size > 0
    && candidate.enabledCountries.every((country) => countryPattern.test(country))
    && candidate.supportedLocales.every((locale) => localePattern.test(locale))
    && mappedCountries.length === countries.size
    && mappedCountries.every((country) => countries.has(country))
    && Object.values(candidate.countryDefaultLocales).every((locale) => locales.has(locale))
    && locales.has(candidate.fallbackLocale);

  if (!valid) {
    throw new Error('invalid_product_catalog_configuration');
  }
  return Object.freeze(candidate);
}

export const productCatalog = validateProductCatalog(rawProductCatalog);
