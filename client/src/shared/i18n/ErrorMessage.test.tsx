import { act, cleanup, render, screen } from '@testing-library/react';
import i18n from './index';
import { ErrorMessage } from './ErrorMessage';
import { errorTranslationKey } from './errorTranslationKey';
import en from './locales/en.json';
import es from './locales/es.json';

afterEach(cleanup);

describe.each([['es', es], ['en', en]] as const)('error presentation in %s', (locale, catalog) => {
  beforeEach(async () => { await i18n.changeLanguage(locale); });

  it.each(Object.keys(es.errors))('translates public code %s', (code) => {
    render(<ErrorMessage code={code} />);
    expect(screen.getByRole('alert').textContent).toBe(catalog.errors[code as keyof typeof es.errors]);
  });

  it.each([undefined, null, {}, '__proto__', 'constructor', 'jdbc:postgresql://secret-db/internal'])
    ('uses translated fallback for unrecognized input %s', (code) => {
      render(<ErrorMessage code={code} />);
      expect(screen.getByRole('alert').textContent).toBe(catalog.errors.unexpected_error);
    });

  it('never displays message, title, detail or violation prose from a failed response', () => {
    const problem = {
      code: 'validation_error',
      title: 'SQL internal title',
      detail: 'private-db password',
      message: 'Socket exception',
      violations: [{ message: 'Server validation prose' }],
    };
    render(<ErrorMessage code={problem.code} />);
    expect(screen.getByRole('alert').textContent).toBe(catalog.errors.validation_error);
    expect(document.body.textContent).not.toMatch(/SQL|private-db|Socket|Server validation/u);
  });
});

it('updates an already rendered error when the language changes', async () => {
  await i18n.changeLanguage('es');
  render(<ErrorMessage code="validation_error" />);
  await act(async () => { await i18n.changeLanguage('en'); });
  expect(screen.getByRole('alert').textContent).toBe(en.errors.validation_error);
});

it('only constructs registered translation keys', () => {
  expect(errorTranslationKey('validation_error')).toBe('errors.validation_error');
  expect(errorTranslationKey('errors.validation_error')).toBe('errors.unexpected_error');
});
