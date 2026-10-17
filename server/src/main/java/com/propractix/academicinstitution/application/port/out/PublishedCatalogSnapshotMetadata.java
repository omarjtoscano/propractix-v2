package com.propractix.academicinstitution.application.port.out;

import com.propractix.academicinstitution.domain.model.CatalogImportId;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public record PublishedCatalogSnapshotMetadata(
        CatalogImportId snapshotId, Instant retrievedAt, URI sourceUrl, String sourceAttribution) {

    public PublishedCatalogSnapshotMetadata {
        Objects.requireNonNull(snapshotId, "snapshotId must not be null");
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
