import { academicInstitutionSearchConfiguration as config, validateAcademicInstitutionSearchConfiguration as validate } from './academicInstitutionSearch';

describe('academic institution search configuration', () => {
  it('uses the approved default page size and freezes validated configuration', () => {
    expect(config.pageSize).toBe(20);
    expect(Object.isFrozen(config)).toBe(true);
    expect(validate({ ...config, pageSize: 50, debounceMs: 0 }).pageSize).toBe(50);
  });
  it.each([
    { pageSize: 0 }, { pageSize: 51 }, { pageSize: 1.5 },
    { debounceMs: -1 }, { debounceMs: Number.NaN },
    { retryFallbackMs: 0 }, { retryFallbackMs: Infinity },
  ])('rejects invalid configuration: %s', (value) => {
    expect(() => validate({ ...config, ...value })).toThrow('invalid_academic_institution_search_configuration');
  });
});
