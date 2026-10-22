import es from './locales/es.json';

type ErrorCode = keyof typeof es.errors;

/** Accept only a stable public code. Never pass exception messages or Problem Details prose. */
export function errorTranslationKey(code: unknown): `errors.${ErrorCode}` {
  const knownCode = typeof code === 'string' && Object.hasOwn(es.errors, code)
    ? code as ErrorCode
    : 'unexpected_error';
  return `errors.${knownCode}`;
}
