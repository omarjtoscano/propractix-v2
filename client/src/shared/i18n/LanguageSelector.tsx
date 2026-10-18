import { useTranslation } from 'react-i18next';
import { productCatalog } from '../../configuration/productCatalog';
import { resolveLocale, saveLocalePreference } from './locale';
import es from './locales/es.json';

export function LanguageSelector() {
  const { t, i18n } = useTranslation();
  const changeLocale = (requested: string) => {
    const locale = resolveLocale({ userLocale: requested });
    saveLocalePreference(locale);
    void i18n.changeLanguage(locale);
  };
  return (
    <label>
      {t('locales.label')}
      <select value={i18n.resolvedLanguage ?? productCatalog.fallbackLocale}
        onChange={(event) => changeLocale(event.currentTarget.value)}>
        {productCatalog.supportedLocales.map((locale) => (
          <option key={locale} value={locale}>{t(`locales.${locale as keyof typeof es.locales}`)}</option>
        ))}
      </select>
    </label>
  );
}
