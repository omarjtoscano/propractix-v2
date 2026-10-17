package com.propractix.academicinstitution.application.port.in;

import com.propractix.academicinstitution.application.query.AcademicInstitutionSearchPage;
import java.util.Objects;

public sealed interface SearchPublishedInstitutionsResult
        permits SearchPublishedInstitutionsResult.Found,
                SearchPublishedInstitutionsResult.SnapshotChanged,
                SearchPublishedInstitutionsResult.CatalogUnavailable {

    record Found(AcademicInstitutionSearchPage page) implements SearchPublishedInstitutionsResult {

        public Found {
            Objects.requireNonNull(page, "page must not be null");
        }
    }

    record SnapshotChanged() implements SearchPublishedInstitutionsResult {}

    record CatalogUnavailable(Failure failure) implements SearchPublishedInstitutionsResult {

        public CatalogUnavailable {
            Objects.requireNonNull(failure, "failure must not be null");
        }
    }

    enum Failure {
        NO_PUBLISHED_CATALOG,
        READ_FAILED
    }
}
