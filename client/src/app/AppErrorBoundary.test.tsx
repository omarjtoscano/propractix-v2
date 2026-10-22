import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from '../shared/i18n';
import { AppErrorBoundary } from './AppErrorBoundary';
import es from '../shared/i18n/locales/es.json';
import en from '../shared/i18n/locales/en.json';

afterEach(() => { cleanup(); vi.restoreAllMocks(); });

it.each([['es', es], ['en', en]] as const)('handles render failures in %s without technical prose',
  async (locale, catalog) => {
    await i18n.changeLanguage(locale);
    vi.spyOn(console, 'error').mockImplementation(() => {});
    let fail = true;
    function FailingView() {
      if (fail) throw new Error('JDBC secret diagnostic');
      return <p>Recovered test content</p>;
    }
    render(<AppErrorBoundary><FailingView /></AppErrorBoundary>);

    expect(screen.getByRole('alert').textContent).toBe(catalog.errors.unexpected_error);
    expect(document.body.textContent).not.toContain('JDBC');
    fail = false;
    await userEvent.click(screen.getByRole('button', { name: catalog.actions.retry }));
    expect(screen.getByText('Recovered test content')).toBeDefined();
    expect(screen.queryByRole('alert')).toBeNull();
  });
