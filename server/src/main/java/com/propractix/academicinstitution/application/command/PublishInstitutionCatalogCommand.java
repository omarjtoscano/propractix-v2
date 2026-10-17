package com.propractix.academicinstitution.application.command;

import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class PublishInstitutionCatalogCommand {

    private final byte[] artifactBytes;
    private final String schemaVersion;
    private final URI sourceUrl;
    private final Instant retrievedAt;
    private final String sourceAttribution;
    private final CatalogArtifactHash declaredArtifactHash;
    private final Optional<CatalogArtifactHash> sourceDownloadHash;

    public PublishInstitutionCatalogCommand(
            byte[] artifactBytes,
            String schemaVersion,
            URI sourceUrl,
            Instant retrievedAt,
            String sourceAttribution,
            CatalogArtifactHash declaredArtifactHash,
            Optional<CatalogArtifactHash> sourceDownloadHash) {
        this.artifactBytes = Arrays.copyOf(
                Objects.requireNonNull(artifactBytes, "artifactBytes must not be null"), artifactBytes.length);
        this.schemaVersion = requireNonBlank(schemaVersion, "schemaVersion");
        this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl must not be null");
        this.retrievedAt = Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        this.sourceAttribution = requireNonBlank(sourceAttribution, "sourceAttribution");
        this.declaredArtifactHash =
                Objects.requireNonNull(declaredArtifactHash, "declaredArtifactHash must not be null");
        this.sourceDownloadHash = Objects.requireNonNull(sourceDownloadHash, "sourceDownloadHash must not be null");

        if (!sourceUrl.isAbsolute()) {
            throw new IllegalArgumentException("sourceUrl must be absolute");
        }
    }

    public byte[] artifactBytes() {
        return Arrays.copyOf(artifactBytes, artifactBytes.length);
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public URI sourceUrl() {
        return sourceUrl;
    }

    public Instant retrievedAt() {
        return retrievedAt;
    }

    public String sourceAttribution() {
        return sourceAttribution;
    }

    public CatalogArtifactHash declaredArtifactHash() {
        return declaredArtifactHash;
    }

    public Optional<CatalogArtifactHash> sourceDownloadHash() {
        return sourceDownloadHash;
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
