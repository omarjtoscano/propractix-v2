import { cleanup, screen } from '@testing-library/react';
import i18n from '../shared/i18n';
import { renderWithQuery } from '../test/render';
import { App } from './App';

describe('H02 application screen', () => {
  afterEach(cleanup);

  it.each([
    ['es', 'Buscar universidad', 'Esta versión utiliza datos sintéticos para pruebas. No son datos reales de RUCT.'],
    ['en', 'Find a university', 'This version uses synthetic test data. It does not contain real RUCT data.'],
  ])('renders the functional screen through the %s catalog', async (locale, title, notice) => {
    await i18n.changeLanguage(locale);
    renderWithQuery(<App />);
    expect(screen.getByRole('heading', { name: 'ProPractix' })).toBeDefined();
    expect(screen.getByRole('heading', { name: title })).toBeDefined();
    expect(screen.getByText(notice)).toBeDefined();
  });
});
