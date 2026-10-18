import { useTranslation } from 'react-i18next';
import { errorTranslationKey } from './errorTranslationKey';

export function ErrorMessage({ code }: Readonly<{ code?: unknown }>) {
  const { t } = useTranslation();
  return <p role="alert">{t(errorTranslationKey(code))}</p>;
}
