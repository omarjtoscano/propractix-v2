import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { productCatalog } from '../../configuration/productCatalog';
import en from './locales/en.json';
import es from './locales/es.json';

const resources = {
  en: { translation: en },
  es: { translation: es },
};
const requestedLocale = globalThis.navigator?.language?.split('-')[0];
const browserLocale = requestedLocale !== undefined
    && productCatalog.supportedLocales.includes(requestedLocale)
  ? requestedLocale
  : productCatalog.fallbackLocale;

void i18n.use(initReactI18next).init({
  resources,
  supportedLngs: productCatalog.supportedLocales,
  fallbackLng: productCatalog.fallbackLocale,
  lng: browserLocale,
  interpolation: {
    escapeValue: false,
  },
  initAsync: false,
});

export default i18n;
