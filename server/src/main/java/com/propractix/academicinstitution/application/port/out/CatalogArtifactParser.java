package com.propractix.academicinstitution.application.port.out;

import java.util.List;
import java.util.Objects;

public interface CatalogArtifactParser {

    ParseResult parse(byte[] exactArtifactBytes);

    sealed interface ParseResult permits ParsedArtifact, RejectedArtifact {}

    record ParsedArtifact(List<CatalogInstitutionCandidate> candidates, int totalRowCount, int omittedRowCount)
            implements ParseResult {

        public ParsedArtifact {
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates must not be null"));
            if (totalRowCount < 0) {
                throw new IllegalArgumentException("totalRowCount must not be negative");
            }
            if (omittedRowCount < 0) {
                throw new IllegalArgumentException("omittedRowCount must not be negative");
            }
            if ((long) candidates.size() + omittedRowCount != totalRowCount) {
                throw new IllegalArgumentException("candidate and omitted counts must equal totalRowCount");
            }
        }
    }

    record RejectedArtifact(Failure failure) implements ParseResult {

        public RejectedArtifact {
            Objects.requireNonNull(failure, "failure must not be null");
        }
    }

    enum Failure {
        INVALID_ENCODING,
        INVALID_SCHEMA,
        INVALID_CONTENT
    }
}
