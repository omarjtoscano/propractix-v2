import { QueryClientProvider, notifyManager } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import type { ReactNode } from 'react';
import { createQueryClient } from '../shared/api/queryClient';

// Keep query notifications in the microtask queue when component tests control timers.
notifyManager.setScheduler(queueMicrotask);

export function renderWithQuery(ui: ReactNode) {
  const client = createQueryClient();
  const result = render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
  return { ...result, client };
}
