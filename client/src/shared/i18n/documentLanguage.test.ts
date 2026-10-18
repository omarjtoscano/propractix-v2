import i18n from './index';
import { syncDocumentLanguage } from './documentLanguage';
import { productCatalog } from '../../configuration/productCatalog';

it.each(['es', 'en'])('sets the document language and title for %s', async (locale) => {
  await i18n.changeLanguage(locale);
  syncDocumentLanguage();
  expect(document.documentElement.lang).toBe(locale);
  expect(document.title).toBe(i18n.t('foundation.title'));
});

it('falls back to the configured language for an unsupported locale', async () => {
  await i18n.changeLanguage('xx');
  syncDocumentLanguage();
  expect(document.documentElement.lang).toBe(productCatalog.fallbackLocale);
  expect(i18n.t('foundation.description')).toBe('Base técnica preparada');
});

// These must fail typechecking if a literal key is absent from the catalogs.
if (false) {
  // @ts-expect-error Unknown translation key.
  i18n.t('missing.translation');
  // @ts-expect-error Inline fallback text cannot make an unknown key valid.
  i18n.t('missing.translation', 'Hardcoded error');
}
