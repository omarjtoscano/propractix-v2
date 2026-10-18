import { resolveLocale, readLocalePreference, saveLocalePreference } from './locale';
import { productCatalog } from '../../configuration/productCatalog';
import i18n from './index';
import es from './locales/es.json';

afterEach(() => { localStorage.clear(); vi.restoreAllMocks(); });

it('gives user preference precedence over company and browser locales', () => {
  expect(resolveLocale({ userLocale: 'en', companyLocale: 'es', companyCountry: 'ES', browserLocales: ['es'] }))
    .toBe('en');
});

it('uses company locale or country mapping before the browser', () => {
  expect(resolveLocale({ companyLocale: 'en', companyCountry: 'ES', browserLocales: ['es'] })).toBe('en');
  expect(resolveLocale({ companyCountry: 'ES', browserLocales: ['en'] })).toBe('es');
});

it('uses configured mappings for other countries without country conditionals', () => {
  const config = { ...productCatalog, supportedLocales: ['es', 'en', 'fr'], countryDefaultLocales: { FR: 'fr' } };
  expect(resolveLocale({ companyCountry: 'FR', browserLocales: ['en'] }, config)).toBe('fr');
});

it('falls back for unsupported company or user locales, independently of browser language', () => {
  expect(resolveLocale({ companyLocale: 'fr', browserLocales: ['en'] })).toBe(productCatalog.fallbackLocale);
  expect(resolveLocale({ companyCountry: 'FR', browserLocales: ['en'] })).toBe(productCatalog.fallbackLocale);
  expect(resolveLocale({ companyCountry: 'constructor' })).toBe(productCatalog.fallbackLocale);
  expect(resolveLocale({ userLocale: 'fr', companyLocale: 'en' })).toBe(productCatalog.fallbackLocale);
});

it('uses browser preferences before company context exists and matches regional variants', () => {
  expect(resolveLocale({ browserLocales: ['fr-FR', 'en-GB'] })).toBe('en');
  expect(resolveLocale({ browserLocales: ['es-ES'] })).toBe('es');
  expect(resolveLocale({})).toBe(productCatalog.fallbackLocale);
});

it('preserves user selection across initialization', () => {
  saveLocalePreference('en');
  expect(readLocalePreference()).toBe('en');
  expect(resolveLocale({ userLocale: readLocalePreference(), browserLocales: ['es'] })).toBe('en');
});

it('does not block the UI when preference storage is unavailable', () => {
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('storage unavailable'); });
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('storage unavailable'); });
  expect(readLocalePreference()).toBeUndefined();
  expect(() => saveLocalePreference('en')).not.toThrow();
});

it('registers a bundle and language label for every enabled locale', () => {
  for (const locale of productCatalog.supportedLocales) {
    expect(i18n.hasResourceBundle(locale, 'translation')).toBe(true);
    expect(Object.hasOwn(es.locales, locale)).toBe(true);
  }
});
