import i18n from './index';
import { productCatalog } from '../../configuration/productCatalog';

export function syncDocumentLanguage(): void {
  document.documentElement.lang = i18n.resolvedLanguage ?? productCatalog.fallbackLocale;
  document.title = i18n.t('foundation.title');
}
