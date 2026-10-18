import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { productCatalog } from '../../configuration/productCatalog';
import en from './locales/en.json';
import es from './locales/es.json';
import { readLocalePreference, resolveLocale } from './locale';

const resources = {
  en: { translation: en },
  es: { translation: es },
};
const initialLocale = resolveLocale({
  userLocale: readLocalePreference(),
  browserLocales: globalThis.navigator?.languages,
});

void i18n.use(initReactI18next).init({
  resources,
  supportedLngs: productCatalog.supportedLocales,
  fallbackLng: productCatalog.fallbackLocale,
  lng: initialLocale,
  interpolation: {
    escapeValue: false,
  },
  initAsync: false,
});

export default i18n;
