import { useTranslation } from 'react-i18next';
import { LanguageSelector } from '../shared/i18n/LanguageSelector';

export function App() {
  const { t } = useTranslation();

  return (
    <main>
      <LanguageSelector />
      <h1>{t('foundation.title')}</h1>
      <p>{t('foundation.description')}</p>
    </main>
  );
}
