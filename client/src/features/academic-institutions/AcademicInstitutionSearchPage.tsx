import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { AcademicInstitutionAutocomplete, type AcademicInstitutionSelection } from './components/AcademicInstitutionAutocomplete';

export function AcademicInstitutionSearchPage() {
  const { t } = useTranslation();
  const [selection, setSelection] = useState<AcademicInstitutionSelection>(null);

  return (
    <section aria-labelledby="academic-institution-heading">
      <h2 id="academic-institution-heading">{t('academicInstitutions.title')}</h2>
      <p>{t('academicInstitutions.description')}</p>
      <p className="catalog-notice">{t('academicInstitutions.synthetic')}</p>
      <p>{t('academicInstitutions.scope')}</p>
      <AcademicInstitutionAutocomplete onChange={setSelection} />
      {selection?.kind === 'catalog' && (
        <p role="status">{t('academicInstitutions.selected', { name: selection.officialName })}</p>
      )}
      {selection?.kind === 'manual' && selection.name !== '' && (
        <p role="status">{t('academicInstitutions.selectedManual', { name: selection.name })}</p>
      )}
    </section>
  );
}
