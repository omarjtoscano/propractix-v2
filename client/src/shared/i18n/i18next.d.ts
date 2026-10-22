import 'i18next';
import type es from './locales/es.json';

declare module 'i18next' {
  interface CustomTypeOptions {
    defaultNS: 'translation';
    strictKeyChecks: true;
    resources: { translation: typeof es };
  }
}
