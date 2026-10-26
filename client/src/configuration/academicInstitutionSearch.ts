import rawConfiguration from './academicInstitutionSearch.json';

export type AcademicInstitutionSearchConfiguration = Readonly<{
  debounceMs: number;
  pageSize: number;
  retryFallbackMs: number;
}>;

export function validateAcademicInstitutionSearchConfiguration(
  candidate: AcademicInstitutionSearchConfiguration,
): AcademicInstitutionSearchConfiguration {
  if (!Number.isSafeInteger(candidate.debounceMs) || candidate.debounceMs < 0
    || !Number.isSafeInteger(candidate.pageSize) || candidate.pageSize < 1 || candidate.pageSize > 50
    || !Number.isSafeInteger(candidate.retryFallbackMs) || candidate.retryFallbackMs < 1000) {
    throw new Error('invalid_academic_institution_search_configuration');
  }
  return Object.freeze({ ...candidate });
}

export const academicInstitutionSearchConfiguration = validateAcademicInstitutionSearchConfiguration(rawConfiguration);
