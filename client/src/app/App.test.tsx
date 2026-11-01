import { cleanup, screen } from '@testing-library/react';
import i18n from '../shared/i18n';
import { renderWithQuery } from '../test/render';
import { App } from './App';

describe('H02 application screen', () => {
  afterEach(cleanup);

  it.each([
    ['es', 'Buscar universidad', 'Consulta la atribución y la fecha del snapshot para conocer el origen de los datos mostrados.',
      'El catálogo es informativo: no acredita afiliación, matrícula, elegibilidad, convenio ni cumplimiento.'],
    ['en', 'Find a university', 'Check the snapshot attribution and retrieval date for the source of the displayed data.',
      'The catalog is for information only: it does not certify affiliation, enrollment, eligibility, an agreement or compliance.'],
  ])('renders the functional screen through the %s catalog', async (locale, title, notice, scope) => {
    await i18n.changeLanguage(locale);
    renderWithQuery(<App />);
    expect(screen.getByRole('heading', { name: 'ProPractix' })).toBeDefined();
    expect(screen.getByRole('heading', { name: title })).toBeDefined();
    expect(screen.getByText(notice)).toBeDefined();
    expect(screen.getByText(scope)).toBeDefined();
  });
});
