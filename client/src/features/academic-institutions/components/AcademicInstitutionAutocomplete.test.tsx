import { act, cleanup, fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from '../../../shared/i18n';
import { renderWithQuery } from '../../../test/render';
import { academicInstitutionSearchConfiguration as config } from '../../../configuration/academicInstitutionSearch';
import { AcademicInstitutionAutocomplete } from './AcademicInstitutionAutocomplete';
import { App } from '../../../app/App';

const first = { id: '11111111-1111-4111-8111-111111111111', officialName: 'Universidad Sintética Áurea', countryCode: 'ES' };
const second = { id: '22222222-2222-4222-8222-222222222222', officialName: 'Universidad Sintética Boreal', countryCode: 'ES' };
const third = { id: '33333333-3333-4333-8333-333333333333', officialName: 'Universidad Sintética Central', countryCode: 'ES' };
const snapshot = { retrievedAt: '2026-09-21T12:00:00Z', sourceUrl: 'https://example.test/catalog', sourceAttribution: 'Fixture sintética ProPractix' };
const page = (items = [first], nextCursor: string | null = null, source = snapshot) => ({ items, nextCursor, snapshot: source });
const response = (body: unknown, status = 200, headers = {}) => new Response(JSON.stringify(body), {
  status, headers: { 'Content-Type': 'application/json', ...headers },
});
const problem = (code: string) => ({ code, title: 'PRIVATE TITLE', detail: 'PRIVATE DETAIL', violations: [{ field: 'query', code: 'PRIVATE VIOLATION' }] });
let sequence = 0;
let fetchMock = vi.fn();
const settle = async (ms = config.debounceMs + 10) => {
  await act(async () => { await vi.advanceTimersByTimeAsync(ms); });
};
function input() { return screen.getByRole('combobox', { name: 'Universidad' }) as HTMLInputElement; }
function start(value = '') { act(() => input().focus()); if (value) fireEvent.change(input(), { target: { value } }); }

beforeEach(async () => {
  vi.useFakeTimers();
  vi.setSystemTime(new Date(Date.UTC(2027, 0, ++sequence)));
  await i18n.changeLanguage('es');
  fetchMock = vi.fn().mockImplementation(async () => response(page()));
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.useRealTimers(); localStorage.clear(); });

it('starts without a request; debounces input and uses the configured page size', async () => {
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  expect(screen.getByText('Abre la búsqueda para consultar el catálogo.')).toBeDefined();
  expect(fetchMock).not.toHaveBeenCalled();
  start('Á');
  await settle(100);
  fireEvent.change(input(), { target: { value: 'Áurea' } });
  expect(screen.getByText('Buscando universidades…')).toBeDefined();
  await settle(config.debounceMs - 1);
  expect(fetchMock).not.toHaveBeenCalled();
  await settle(11);
  expect(fetchMock).toHaveBeenCalledTimes(1);
  const url = new URL(fetchMock.mock.calls[0][0].url);
  expect(url.searchParams.get('query')).toBe('Áurea');
  expect(url.searchParams.get('limit')).toBe(String(config.pageSize));
  expect(screen.getByRole('option', { name: first.officialName })).toBeDefined();
});

it('lists the first page for blank input and announces an empty result', async () => {
  fetchMock.mockResolvedValue(response(page([])));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start();
  await settle();
  expect(new URL(fetchMock.mock.calls[0][0].url).searchParams.has('cursor')).toBe(false);
  expect(screen.getAllByRole('status')).toContain(screen.getByText('No hay universidades que coincidan con la búsqueda.'));
});

it('supports keyboard navigation, active descendant, Escape, Tab and selection by UUID', async () => {
  const onChange = vi.fn();
  vi.useRealTimers();
  const user = userEvent.setup();
  fetchMock.mockResolvedValue(response(page([first, second])));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={onChange} />);
  await user.tab();
  expect(document.activeElement).toBe(input());
  await screen.findByRole('option', { name: first.officialName });
  expect(input().getAttribute('aria-expanded')).toBe('true');
  expect(document.getElementById(input().getAttribute('aria-controls')!)).toBe(screen.getByRole('listbox'));
  await user.keyboard('{ArrowDown}{ArrowDown}');
  const option = screen.getByRole('option', { name: second.officialName });
  expect(input().getAttribute('aria-activedescendant')).toBe(option.id);
  expect(option.getAttribute('aria-selected')).toBe('true');
  expect(document.activeElement).toBe(input());
  await user.keyboard('{Escape}');
  expect(input().getAttribute('aria-expanded')).toBe('false');
  expect(input().hasAttribute('aria-activedescendant')).toBe(false);
  await user.keyboard('{ArrowUp}{Enter}');
  expect(onChange).toHaveBeenLastCalledWith({ kind: 'catalog', id: second.id, officialName: second.officialName });
  expect(input().value).toBe(second.officialName);
  expect(input().getAttribute('aria-expanded')).toBe('false');
  expect(document.activeElement).toBe(input());
  expect(screen.getByText(snapshot.sourceAttribution)).toBeDefined();
  await user.tab();
  expect(document.activeElement).not.toBe(input());
});

it.each(['pointer', 'keyboard'])('restores focus after paging and selection without reopening or querying, then allows %s reopening', async (reopen) => {
  vi.useRealTimers();
  const user = userEvent.setup();
  const onChange = vi.fn();
  fetchMock.mockResolvedValueOnce(response(page([first], 'cursor_1')))
    .mockResolvedValueOnce(response(page([second], 'cursor_2')))
    .mockImplementation(async () => response(page([second])));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={onChange} />);

  await user.click(input());
  await screen.findByRole('option', { name: first.officialName });
  expect(document.activeElement).toBe(input());
  const more = screen.getByRole('button', { name: 'Cargar más universidades' });
  await user.click(more);
  await screen.findByRole('option', { name: second.officialName });
  expect(document.activeElement).toBe(more);
  expect(fetchMock).toHaveBeenCalledTimes(2);
  expect(new URL(fetchMock.mock.calls[1][0].url).searchParams.get('cursor')).toBe('cursor_1');

  await user.click(screen.getByRole('option', { name: second.officialName }));
  expect(document.activeElement).toBe(input());
  expect(input().value).toBe(second.officialName);
  expect(input().getAttribute('aria-expanded')).toBe('false');
  expect(input().hasAttribute('aria-activedescendant')).toBe(false);
  expect(screen.queryByRole('listbox')).toBeNull();
  expect(onChange).toHaveBeenCalledExactlyOnceWith({ kind: 'catalog', id: second.id, officialName: second.officialName });
  await act(async () => { await new Promise((resolve) => setTimeout(resolve, config.debounceMs + 50)); });
  expect(fetchMock).toHaveBeenCalledTimes(2);
  expect(input().getAttribute('aria-expanded')).toBe('false');

  if (reopen === 'pointer') await user.click(input());
  else await user.keyboard('{ArrowDown}');
  expect(input().getAttribute('aria-expanded')).toBe('true');
  await screen.findByRole('option', { name: second.officialName });
  expect(fetchMock).toHaveBeenCalledTimes(3);
  expect(new URL(fetchMock.mock.calls[2][0].url).searchParams.has('cursor')).toBe(false);
  await user.keyboard('{ArrowDown}{Enter}');
  expect(document.activeElement).toBe(input());
  expect(input().getAttribute('aria-expanded')).toBe('false');
  expect(onChange).toHaveBeenCalledTimes(2);
  expect(onChange).toHaveBeenLastCalledWith({ kind: 'catalog', id: second.id, officialName: second.officialName });
  await act(async () => { await new Promise((resolve) => setTimeout(resolve, config.debounceMs + 50)); });
  expect(fetchMock).toHaveBeenCalledTimes(3);
});

it('selects by pointer and clears the selection on further input', async () => {
  const onChange = vi.fn();
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={onChange} />);
  start(); await settle();
  fireEvent.click(screen.getByRole('option', { name: first.officialName }));
  expect(input().getAttribute('aria-expanded')).toBe('false');
  expect(onChange).toHaveBeenLastCalledWith({ kind: 'catalog', id: first.id, officialName: first.officialName });
  fireEvent.change(input(), { target: { value: 'changed' } });
  expect(onChange).toHaveBeenLastCalledWith(null);
  expect(screen.queryByRole('option')).toBeNull();
});

it('pages by opaque cursor, deduplicates IDs and prevents double page requests', async () => {
  fetchMock.mockResolvedValueOnce(response(page([first], 'cursor_1')))
    .mockResolvedValueOnce(response(page([first, second])));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start(); await settle();
  const more = screen.getByRole('button', { name: 'Cargar más universidades' });
  fireEvent.click(more); fireEvent.click(more);
  await settle(10);
  expect(fetchMock).toHaveBeenCalledTimes(2);
  expect(new URL(fetchMock.mock.calls[1][0].url).searchParams.get('cursor')).toBe('cursor_1');
  expect(screen.getAllByRole('option')).toHaveLength(2);
  expect(screen.queryByRole('button', { name: 'Cargar más universidades' })).toBeNull();
  expect(screen.getByText('2 universidades disponibles.')).toBeDefined();
});

it('hides previous pages immediately when text changes and ignores late responses', async () => {
  let resolveLate!: (response: Response) => void;
  fetchMock.mockImplementationOnce(() => new Promise<Response>((resolve) => { resolveLate = resolve; }))
    .mockResolvedValueOnce(response(page([second])));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start('old'); await settle();
  const oldSignal = (fetchMock.mock.calls[0][0] as Request).signal;
  fireEvent.change(input(), { target: { value: 'new' } });
  await settle();
  expect(oldSignal.aborted).toBe(true);
  expect(screen.getByRole('option', { name: second.officialName })).toBeDefined();
  await act(async () => { resolveLate(response(page([first]))); });
  await settle(10);
  expect(screen.queryByRole('option', { name: first.officialName })).toBeNull();
});

it('drops all previous pages and restarts without cursor after catalog_snapshot_changed', async () => {
  fetchMock.mockResolvedValueOnce(response(page([first], 'old_cursor')))
    .mockResolvedValueOnce(response(problem('catalog_snapshot_changed'), 409))
    .mockResolvedValueOnce(response(page([third], null, { ...snapshot, retrievedAt: '2026-10-25T12:00:00Z' })));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start(); await settle();
  fireEvent.click(screen.getByRole('button', { name: 'Cargar más universidades' }));
  await settle(30);
  expect(fetchMock).toHaveBeenCalledTimes(3);
  expect(new URL(fetchMock.mock.calls[2][0].url).searchParams.has('cursor')).toBe(false);
  expect(screen.queryByRole('option', { name: first.officialName })).toBeNull();
  expect(screen.getByRole('option', { name: third.officialName })).toBeDefined();
  expect(screen.getByText('El catálogo se ha actualizado. La búsqueda se ha reiniciado.')).toBeDefined();
});

it('also restarts if successful pages unexpectedly carry different snapshot metadata', async () => {
  fetchMock.mockResolvedValueOnce(response(page([first], 'cursor_1')))
    .mockResolvedValueOnce(response(page([second], null, { ...snapshot, retrievedAt: '2026-10-25T12:00:00Z' })))
    .mockResolvedValueOnce(response(page([third])));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start(); await settle();
  fireEvent.click(screen.getByRole('button', { name: 'Cargar más universidades' }));
  await settle(30);
  expect(screen.getAllByRole('option')).toHaveLength(1);
  expect(screen.getByRole('option', { name: third.officialName })).toBeDefined();
});

it('bounds automatic snapshot restarts to one until the user retries', async () => {
  fetchMock.mockImplementation(async () => response(problem('catalog_snapshot_changed'), 409));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start(); await settle(); await settle(30);
  expect(fetchMock).toHaveBeenCalledTimes(2);
  await settle(60_000);
  expect(fetchMock).toHaveBeenCalledTimes(2);
  expect(screen.getByRole('button', { name: 'Reintentar búsqueda' })).toBeDefined();
});

it.each([
  [400, 'validation_error', 'Revisa los datos introducidos.'],
  [503, 'academic_institution_catalog_unavailable', 'El catálogo de universidades no está disponible temporalmente.'],
  [503, 'rate_limiter_unavailable', 'El servicio no está disponible temporalmente. Vuelve a intentarlo más tarde.'],
  [500, 'unknown_code', 'No se ha podido completar la operación. Vuelve a intentarlo.'],
])('renders HTTP %s through the code catalog and offers explicit retry', async (status, code, message) => {
  fetchMock.mockResolvedValueOnce(response(problem(code), status)).mockResolvedValue(response(page()));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start(); await settle();
  expect(screen.getByRole('alert').textContent).toBe(message);
  expect(document.body.textContent).not.toContain('PRIVATE');
  expect(screen.queryByRole('option')).toBeNull();
  await settle(1500);
  fireEvent.click(screen.getByRole('button', { name: 'Reintentar búsqueda' }));
  await settle(10);
  expect(screen.getByRole('option', { name: first.officialName })).toBeDefined();
});

it.each([[429, 'rate_limited'], [503, 'rate_limiter_unavailable']])('honors Retry-After on %s across typing, focus, reconnect and remount; only explicit retry sends again', async (status, code) => {
  fetchMock.mockResolvedValueOnce(response(problem(code as string), status as number, { 'Retry-After': '5' }))
    .mockResolvedValue(response(page()));
  const view = renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start(); await settle();
  expect(screen.getByRole('alert')).toBeDefined();
  expect((screen.getByRole('button', { name: 'Reintentar búsqueda' }) as HTMLButtonElement).disabled).toBe(true);
  fireEvent.change(input(), { target: { value: 'other' } });
  await settle();
  view.unmount();
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start(); await settle();
  fireEvent(window, new Event('focus')); fireEvent(window, new Event('online'));
  expect(fetchMock).toHaveBeenCalledTimes(1);
  await settle(5000);
  expect(fetchMock).toHaveBeenCalledTimes(1);
  expect((screen.getByRole('button', { name: 'Reintentar búsqueda' }) as HTMLButtonElement).disabled).toBe(false);
  fireEvent.click(screen.getByRole('button', { name: 'Reintentar búsqueda' }));
  await settle(10);
  expect(fetchMock).toHaveBeenCalledTimes(2);
});

it('offers the manual value with focus and no persistence or catalog write', async () => {
  const onChange = vi.fn();
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={onChange} />);
  fireEvent.click(screen.getByRole('button', { name: 'No encuentro mi universidad' }));
  const manual = screen.getByRole('textbox', { name: 'Nombre de mi universidad' });
  expect(document.activeElement).toBe(manual);
  fireEvent.change(manual, { target: { value: 'Mi institución' } });
  expect(onChange).toHaveBeenLastCalledWith({ kind: 'manual', name: 'Mi institución' });
  await settle();
  expect(fetchMock).not.toHaveBeenCalled();
  expect(localStorage.length).toBe(0);
});

it('changes ES/EN with results visible without refetching the catalog', async () => {
  renderWithQuery(<App />);
  start(); await settle();
  const status = screen.getByText('1 universidad disponible.');
  expect(status.getAttribute('aria-live')).toBe('polite');
  fireEvent.change(screen.getByRole('combobox', { name: 'Idioma' }), { target: { value: 'en' } });
  expect(screen.getByRole('combobox', { name: 'University' })).toBeDefined();
  expect(screen.getByText('1 university available.')).toBeDefined();
  expect(screen.getByText(first.officialName)).toBeDefined();
  expect(screen.getByText(snapshot.sourceAttribution)).toBeDefined();
  expect(screen.getByRole('link', { name: 'View snapshot source' }).getAttribute('href')).toBe(snapshot.sourceUrl);
  expect(fetchMock).toHaveBeenCalledTimes(1);
});


it('hides already loaded pages on pagination failure and retries from the first page', async () => {
  fetchMock.mockResolvedValueOnce(response(page([first], 'old_cursor')))
    .mockResolvedValueOnce(response(problem('academic_institution_catalog_unavailable'), 503, { 'Retry-After': '2' }))
    .mockResolvedValueOnce(response(page([second])));
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={vi.fn()} />);
  start(); await settle();
  fireEvent.click(screen.getByRole('button', { name: 'Cargar más universidades' }));
  await settle(10);
  expect(screen.queryByRole('option')).toBeNull();
  expect(screen.queryByText(snapshot.sourceAttribution)).toBeNull();
  await settle(2100);
  expect(fetchMock).toHaveBeenCalledTimes(2);
  fireEvent.click(screen.getByRole('button', { name: 'Reintentar búsqueda' }));
  await settle(10);
  expect(new URL(fetchMock.mock.calls[2][0].url).searchParams.has('cursor')).toBe(false);
  expect(screen.getAllByRole('option')).toHaveLength(1);
  expect(screen.getByRole('option', { name: second.officialName })).toBeDefined();
});

it('translates an already visible API error with the existing language selector', async () => {
  fetchMock.mockResolvedValue(response(problem('validation_error'), 400));
  renderWithQuery(<App />);
  start('invalid'); await settle();
  expect(screen.getByRole('alert').textContent).toBe('Revisa los datos introducidos.');
  fireEvent.change(screen.getByRole('combobox', { name: 'Idioma' }), { target: { value: 'en' } });
  expect(screen.getByRole('alert').textContent).toBe('Check the information you entered.');
  expect(document.body.textContent).not.toContain('PRIVATE');
  expect(fetchMock).toHaveBeenCalledTimes(1);
});


it('can leave manual entry and reopen the catalog using only the keyboard', async () => {
  const onChange = vi.fn();
  renderWithQuery(<AcademicInstitutionAutocomplete onChange={onChange} />);
  fireEvent.click(screen.getByRole('button', { name: 'No encuentro mi universidad' }));
  fireEvent.change(screen.getByRole('textbox', { name: 'Nombre de mi universidad' }), { target: { value: 'Manual' } });
  act(() => input().focus());
  fireEvent.keyDown(input(), { key: 'ArrowDown' });
  await settle();
  expect(onChange).toHaveBeenLastCalledWith(null);
  expect(screen.queryByRole('textbox', { name: 'Nombre de mi universidad' })).toBeNull();
  expect(screen.getByRole('option', { name: first.officialName })).toBeDefined();
  fireEvent.keyDown(input(), { key: 'ArrowDown' });
  fireEvent.keyDown(input(), { key: 'Enter' });
  expect(onChange).toHaveBeenLastCalledWith({ kind: 'catalog', id: first.id, officialName: first.officialName });
});
