package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogImportMetadataTest {

    private static final CatalogArtifactHash ARTIFACT_HASH = new CatalogArtifactHash("a".repeat(64));
    private static final CatalogArtifactHash DOWNLOAD_HASH = new CatalogArtifactHash("b".repeat(64));

    @Test
    void requiresTheMandatoryMetadata() {
        assertThrows(
                NullPointerException.class,
                () -> new CatalogImportMetadata(
                        null, Instant.parse("2026-09-21T10:15:30Z"), "RUCT", ARTIFACT_HASH, Optional.empty()));
        assertThrows(
                NullPointerException.class,
                () -> new CatalogImportMetadata(
                        URI.create("https://example.test/ruct"), null, "RUCT", ARTIFACT_HASH, Optional.empty()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CatalogImportMetadata(
                        URI.create("https://example.test/ruct"),
                        Instant.parse("2026-09-21T10:15:30Z"),
                        " ",
                        ARTIFACT_HASH,
                        Optional.empty()));
        assertThrows(
                NullPointerException.class,
                () -> new CatalogImportMetadata(
                        URI.create("https://example.test/ruct"),
                        Instant.parse("2026-09-21T10:15:30Z"),
                        "RUCT",
                        null,
                        Optional.empty()));
    }

    @Test
    void keepsTheOptionalDownloadHashSeparateFromThePublishedArtifactHash() {
        CatalogImportMetadata metadata = metadata(Optional.of(DOWNLOAD_HASH));

        assertEquals(ARTIFACT_HASH, metadata.artifactHash());
        assertEquals(Optional.of(DOWNLOAD_HASH), metadata.sourceDownloadHash());
        assertNotEquals(metadata.artifactHash(), metadata.sourceDownloadHash().orElseThrow());
    }

    @Test
    void permitsEqualHashValuesWithoutConflatingTheirSeparateMeanings() {
        CatalogImportMetadata metadata = metadata(Optional.of(ARTIFACT_HASH));

        assertEquals(ARTIFACT_HASH, metadata.artifactHash());
        assertEquals(Optional.of(ARTIFACT_HASH), metadata.sourceDownloadHash());
    }

    @Test
    void permitsAnAbsentDownloadHashWithoutInferringOne() {
        CatalogImportMetadata metadata = metadata(Optional.empty());

        assertEquals(Optional.empty(), metadata.sourceDownloadHash());
    }

    @Test
    void equalityAndHashCodeUseEveryMetadataField() {
        CatalogImportMetadata first = metadata(Optional.empty());
        CatalogImportMetadata equal = metadata(Optional.empty());
        CatalogImportMetadata different = metadata(Optional.of(DOWNLOAD_HASH));

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, different);
    }

    private static CatalogImportMetadata metadata(Optional<CatalogArtifactHash> downloadHash) {
        return new CatalogImportMetadata(
                URI.create("https://example.test/ruct"),
                Instant.parse("2026-09-21T10:15:30Z"),
                "Origen de los datos: RUCT",
                ARTIFACT_HASH,
                downloadHash);
    }
}
