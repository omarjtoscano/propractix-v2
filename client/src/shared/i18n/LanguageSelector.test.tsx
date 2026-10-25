import { cleanup, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from './index';
import { App } from '../../app/App';
import { renderWithQuery } from '../../test/render';
import { readLocalePreference } from './locale';

afterEach(() => { cleanup(); localStorage.clear(); });

it('switches the rendered shell from Spanish to English and saves the selection', async () => {
  await i18n.changeLanguage('es');
  renderWithQuery(<App />);
  await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Idioma' }), 'en');
  expect(screen.getByText('Find a university')).toBeDefined();
  expect(screen.getByRole('combobox', { name: 'Language' })).toBeDefined();
  expect(readLocalePreference()).toBe('en');
});

it('switches back to Spanish', async () => {
  await i18n.changeLanguage('en');
  renderWithQuery(<App />);
  await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Language' }), 'es');
  expect(screen.getByText('Buscar universidad')).toBeDefined();
  expect(readLocalePreference()).toBe('es');
});
