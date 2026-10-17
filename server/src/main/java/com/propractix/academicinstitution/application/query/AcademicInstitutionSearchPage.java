package com.propractix.academicinstitution.application.query;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AcademicInstitutionSearchPage(
        List<AcademicInstitutionSearchItem> items,
        Optional<InstitutionSearchCursor> nextCursor,
        Instant retrievedAt,
        URI sourceUrl,
        String sourceAttribution) {

    public AcademicInstitutionSearchPage {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        Objects.requireNonNull(nextCursor, "nextCursor must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        Objects.requireNonNull(sourceAttribution, "sourceAttribution must not be null");
        if (!sourceUrl.isAbsolute()) {
            throw new IllegalArgumentException("sourceUrl must be absolute");
        }
        if (sourceAttribution.isBlank()) {
            throw new IllegalArgumentException("sourceAttribution must not be blank");
        }
    }
}
