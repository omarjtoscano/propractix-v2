import { cleanup, render, screen } from '@testing-library/react';
import i18n from '../shared/i18n';
import { App } from './App';

describe('technical shell', () => {
  afterEach(cleanup);

  it.each([
    ['es', 'Base técnica preparada'],
    ['en', 'Technical foundation ready'],
  ])('renders copy through the %s i18n catalog', async (locale, description) => {
    await i18n.changeLanguage(locale);
    render(<App />);

    expect(screen.getByRole('heading', { name: 'ProPractix' })).toBeDefined();
    expect(screen.getByText(description)).toBeDefined();
  });
});
