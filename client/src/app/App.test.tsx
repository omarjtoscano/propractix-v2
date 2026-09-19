import { render, screen } from '@testing-library/react';
import i18n from '../shared/i18n';
import { App } from './App';

describe('technical shell', () => {
  it('renders copy through the Spanish i18n catalog', () => {
    void i18n.changeLanguage('es');
    render(<App />);

    expect(screen.getByRole('heading', { name: 'ProPractix' })).toBeDefined();
    expect(screen.getByText('Base técnica preparada')).toBeDefined();
  });
});
