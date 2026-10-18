package com.propractix.academicinstitution.adapter.in.commandline;

import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record CatalogImportCommandLineDefinition(
        boolean enabled,
        Path artifactPath,
        String schemaVersion,
        URI sourceUrl,
        Instant retrievedAt,
        String sourceAttribution,
        CatalogArtifactHash artifactHash,
        Optional<CatalogArtifactHash> sourceDownloadHash) {

    public CatalogImportCommandLineDefinition {
        Objects.requireNonNull(sourceDownloadHash, "sourceDownloadHash must not be null");
        if (enabled) {
            Objects.requireNonNull(artifactPath, "artifactPath must not be null when import is enabled");
            Objects.requireNonNull(schemaVersion, "schemaVersion must not be null when import is enabled");
            Objects.requireNonNull(sourceUrl, "sourceUrl must not be null when import is enabled");
            Objects.requireNonNull(retrievedAt, "retrievedAt must not be null when import is enabled");
            Objects.requireNonNull(sourceAttribution, "sourceAttribution must not be null when import is enabled");
            Objects.requireNonNull(artifactHash, "artifactHash must not be null when import is enabled");
            if (schemaVersion.isBlank() || sourceAttribution.isBlank() || !sourceUrl.isAbsolute()) {
                throw new IllegalArgumentException("enabled import metadata must be complete");
            }
        }
    }
}
