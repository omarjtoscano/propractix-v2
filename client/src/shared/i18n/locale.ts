import { productCatalog, type ProductCatalog } from '../../configuration/productCatalog';

export type LocaleContext = Readonly<{
  userLocale?: string;
  companyLocale?: string;
  companyCountry?: string;
  browserLocales?: readonly string[];
}>;

export function resolveLocale(context: LocaleContext, catalog: ProductCatalog = productCatalog): string {
  const supported = (requested: string): string | undefined => {
    const exact = catalog.supportedLocales.find((locale) => locale.toLowerCase() === requested.toLowerCase());
    return exact ?? catalog.supportedLocales.find((locale) => locale === requested.split('-')[0].toLowerCase());
  };
  if (context.userLocale !== undefined) return supported(context.userLocale) ?? catalog.fallbackLocale;
  if (context.companyLocale !== undefined) return supported(context.companyLocale) ?? catalog.fallbackLocale;
  if (context.companyCountry !== undefined) {
    const configured = Object.hasOwn(catalog.countryDefaultLocales, context.companyCountry)
      ? catalog.countryDefaultLocales[context.companyCountry]
      : undefined;
    return configured === undefined ? catalog.fallbackLocale : supported(configured) ?? catalog.fallbackLocale;
  }
  for (const requested of context.browserLocales ?? []) {
    const locale = supported(requested);
    if (locale !== undefined) return locale;
  }
  return catalog.fallbackLocale;
}

const preferenceKey = 'propractix.ui.locale';

export function readLocalePreference(): string | undefined {
  try {
    return globalThis.localStorage?.getItem(preferenceKey) ?? undefined;
  } catch {
    return undefined;
  }
}

export function saveLocalePreference(locale: string): void {
  try {
    globalThis.localStorage?.setItem(preferenceKey, locale);
  } catch {
    // A browser that denies storage can still change language for the current session.
  }
}
