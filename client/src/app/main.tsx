import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import i18n from '../shared/i18n';
import { App } from './App';

document.documentElement.lang = i18n.resolvedLanguage ?? 'es';
document.title = i18n.t('foundation.title');

const root = document.getElementById('root');
if (root === null) {
  throw new Error('root_element_missing');
}

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
