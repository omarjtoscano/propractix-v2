package com.propractix.academicinstitution.application.port.out;

import com.propractix.academicinstitution.application.query.InstitutionSearchCursor;
import com.propractix.academicinstitution.application.query.NormalizedInstitutionSearchTerm;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface PublishedInstitutionQueryRepository {

    /**
     * Reads at most {@code fetchLimit} rows already filtered and ordered by search name plus internal institution ID.
     * Cursor snapshot validation and the read itself must observe one consistent published snapshot. A changed cursor
     * snapshot is reported as {@link SnapshotChanged}; no implementation may load the complete catalog to paginate.
     */
    SearchOutcome search(SearchRequest request);

    record SearchRequest(
            Optional<NormalizedInstitutionSearchTerm> term,
            Optional<InstitutionSearchCursor> cursor,
            int fetchLimit) {

        public SearchRequest {
            Objects.requireNonNull(term, "term must not be null");
            Objects.requireNonNull(cursor, "cursor must not be null");
            if (fetchLimit < 2 || fetchLimit > 51) {
                throw new IllegalArgumentException("fetchLimit must be between 2 and 51");
            }
        }
    }

    sealed interface SearchOutcome permits Page, SnapshotChanged, CatalogUnavailable {}

    record Page(PublishedCatalogSnapshotMetadata snapshot, List<PublishedInstitutionSearchRow> rows)
            implements SearchOutcome {

        public Page {
            Objects.requireNonNull(snapshot, "snapshot must not be null");
            rows = List.copyOf(Objects.requireNonNull(rows, "rows must not be null"));
        }
    }

    record SnapshotChanged() implements SearchOutcome {}

    record CatalogUnavailable(Failure failure) implements SearchOutcome {

        public CatalogUnavailable {
            Objects.requireNonNull(failure, "failure must not be null");
        }
    }

    enum Failure {
        NO_PUBLISHED_CATALOG,
        READ_FAILED
    }
}
