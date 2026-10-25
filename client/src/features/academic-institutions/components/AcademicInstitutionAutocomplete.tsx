import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { AcademicInstitutionSearchItem, AcademicInstitutionSnapshot } from '../../../shared/api/generated';
import { ErrorMessage } from '../../../shared/i18n/ErrorMessage';
import { useAcademicInstitutionSearch } from '../api/useAcademicInstitutionSearch';

export type AcademicInstitutionSelection =
  | Readonly<{ kind: 'catalog'; id: string; officialName: string }>
  | Readonly<{ kind: 'manual'; name: string }>
  | null;

type Props = Readonly<{
  onChange: (value: AcademicInstitutionSelection) => void;
}>;

/** A catalog selection is emitted by internal UUID; a manual value never becomes a catalog item. */
export function AcademicInstitutionAutocomplete({ onChange }: Props) {
  const { t, i18n } = useTranslation();
  const id = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const manualRef = useRef<HTMLInputElement>(null);
  const [text, setText] = useState('');
  const [opened, setOpened] = useState(false);
  const [started, setStarted] = useState(false);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [manual, setManual] = useState(false);
  const [selectedSnapshot, setSelectedSnapshot] = useState<AcademicInstitutionSnapshot | undefined>(undefined);
  const [manualName, setManualName] = useState('');
  const search = useAcademicInstitutionSearch(text, started && !manual);
  const snapshot = search.snapshot ?? (!started ? selectedSnapshot : undefined);
  useEffect(() => { if (manual) manualRef.current?.focus(); }, [manual]);
  const activeIndex = search.items.findIndex((item) => item.id === activeId);
  const expanded = opened && !manual;
  const activeOption = expanded && activeIndex >= 0 ? `${id}-option-${activeId}` : undefined;

  function select(item: AcademicInstitutionSearchItem) {
    setSelectedSnapshot(search.snapshot);
    setText(item.officialName);
    setOpened(false);
    setStarted(false);
    setActiveId(null);
    onChange({ kind: 'catalog', id: item.id, officialName: item.officialName });
    inputRef.current?.focus();
  }

  function navigate(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Escape') {
      event.preventDefault();
      setOpened(false);
      setActiveId(null);
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (manual) { setManual(false); onChange(null); }
      setOpened(true);
      setStarted(true);
      const next = event.key === 'ArrowDown'
        ? Math.min(activeIndex + 1, search.items.length - 1)
        : activeIndex < 0 ? search.items.length - 1 : Math.max(activeIndex - 1, 0);
      const item = search.items[next];
      setActiveId(item?.id ?? null);
      if (item) document.getElementById(`${id}-option-${item.id}`)?.scrollIntoView?.({ block: 'nearest' });
    } else if (event.key === 'Enter' && expanded && activeIndex >= 0) {
      event.preventDefault();
      select(search.items[activeIndex]);
    }
  }

  const announcement = !started ? t('academicInstitutions.initial')
    : search.loading ? t('academicInstitutions.loading')
      : search.error ? ''
        : search.items.length === 0 ? t('academicInstitutions.empty')
          : t('academicInstitutions.results', { count: search.items.length });

  return (
    <div className="institution-search" onBlur={(event) => {
      if (!event.currentTarget.contains(event.relatedTarget)) {
        setOpened(false);
        setActiveId(null);
      }
    }}>
      <label htmlFor={`${id}-input`}>{t('academicInstitutions.label')}</label>
      <p id={`${id}-hint`}>{t('academicInstitutions.hint')}</p>
      <input
        ref={inputRef}
        id={`${id}-input`}
        role="combobox"
        aria-autocomplete="list"
        aria-expanded={expanded}
        aria-controls={`${id}-list`}
        aria-activedescendant={activeOption}
        aria-describedby={`${id}-hint ${id}-status`}
        autoComplete="off"
        value={text}
        onFocus={() => {
          if (!manual) { setStarted(true); setOpened(true); }
        }}
        onClick={() => {
          if (manual) onChange(null);
          setManual(false); setStarted(true); setOpened(true);
        }}
        onChange={(event) => {
          setText(event.target.value);
          setSelectedSnapshot(undefined);
          setManual(false);
          setStarted(true);
          setOpened(true);
          setActiveId(null);
          onChange(null);
        }}
        onKeyDown={navigate}
      />
      <p id={`${id}-status`} role="status" aria-live="polite" aria-atomic="true">{announcement}</p>
      {search.snapshotChanged && <p role="status">{t('academicInstitutions.changed')}</p>}
      {search.error && !manual && <ErrorMessage code={search.error.code} />}
      {search.waitSeconds > 0 && !manual && (
        <p role="status">{t('academicInstitutions.waiting', { count: search.waitSeconds })}</p>
      )}
      <ul id={`${id}-list`} role="listbox" aria-label={t('academicInstitutions.label')} hidden={!expanded} aria-busy={search.loading}>
        {search.items.map((item) => (
          <li
            key={item.id}
            id={`${id}-option-${item.id}`}
            role="option"
            aria-selected={item.id === activeId}
            onPointerDown={(event) => event.preventDefault()}
            onClick={() => select(item)}
          >{item.officialName}</li>
        ))}
      </ul>
      {expanded && search.hasNextPage && !search.error && (
        <button type="button" disabled={search.loading || search.waitSeconds > 0} onClick={search.loadMore}>
          {t('academicInstitutions.more')}
        </button>
      )}
      {search.error && !manual && (
        <button type="button" disabled={search.loading || search.waitSeconds > 0} onClick={search.retry}>
          {t('academicInstitutions.retry')}
        </button>
      )}
      {snapshot && !manual && (
        <aside aria-label={t('academicInstitutions.attribution')}>
          <p>{snapshot.sourceAttribution}</p>
          <p>{t('academicInstitutions.retrievedAt', {
            date: new Intl.DateTimeFormat(i18n.resolvedLanguage, { dateStyle: 'medium', timeZone: 'UTC' })
              .format(new Date(snapshot.retrievedAt)),
          })}</p>
          <a href={snapshot.sourceUrl} target="_blank" rel="noopener noreferrer">{t('academicInstitutions.source')}</a>
        </aside>
      )}
      <button type="button" onClick={() => {
        setManual(true);
        setText('');
        setSelectedSnapshot(undefined);
        setStarted(false);
        setOpened(false);
        setActiveId(null);
        setManualName('');
        onChange({ kind: 'manual', name: '' });
      }}>{t('academicInstitutions.manual')}</button>
      {manual && (
        <div>
          <label htmlFor={`${id}-manual`}>{t('academicInstitutions.manualLabel')}</label>
          <input ref={manualRef} id={`${id}-manual`} aria-describedby={`${id}-manual-help`} value={manualName} onChange={(event) => {
            setManualName(event.target.value);
            onChange({ kind: 'manual', name: event.target.value });
          }} />
          <p id={`${id}-manual-help`}>{t('academicInstitutions.manualHelp')}</p>
        </div>
      )}
    </div>
  );
}
