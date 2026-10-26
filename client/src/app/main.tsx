import { StrictMode } from 'react';
import { QueryClientProvider } from '@tanstack/react-query';
import { createQueryClient } from '../shared/api/queryClient';
import { createRoot } from 'react-dom/client';
import i18n from '../shared/i18n';
import { syncDocumentLanguage } from '../shared/i18n/documentLanguage';
import { App } from './App';
import { AppErrorBoundary } from './AppErrorBoundary';

syncDocumentLanguage();
i18n.on('languageChanged', syncDocumentLanguage);

const root = document.getElementById('root');
if (root === null) {
  throw new Error('root_element_missing');
}

const queryClient = createQueryClient();

createRoot(root).render(
  <StrictMode>
    <AppErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </AppErrorBoundary>
  </StrictMode>,
);
