package com.propractix.academicinstitution.domain.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record CatalogImportMetadata(
        URI sourceUrl,
        Instant retrievedAt,
        String sourceAttribution,
        CatalogArtifactHash artifactHash,
        Optional<CatalogArtifactHash> sourceDownloadHash) {

    public CatalogImportMetadata {
        Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        Objects.requireNonNull(sourceAttribution, "sourceAttribution must not be null");
        Objects.requireNonNull(artifactHash, "artifactHash must not be null");
        Objects.requireNonNull(sourceDownloadHash, "sourceDownloadHash must not be null");

        if (!sourceUrl.isAbsolute()) {
            throw new IllegalArgumentException("sourceUrl must be absolute");
        }
        if (sourceAttribution.isBlank()) {
            throw new IllegalArgumentException("sourceAttribution must not be blank");
        }
    }
}
