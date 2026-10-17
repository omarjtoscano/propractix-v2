package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogImportTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-21T10:15:30Z");

    @Test
    void aPublishedImportCanBecomeSuperseded() {
        CatalogImport catalogImport = publishedImport("a".repeat(64));

        catalogImport.markSuperseded(PUBLISHED_AT.plusSeconds(60));

        assertEquals(CatalogImport.Status.SUPERSEDED, catalogImport.status());
        assertEquals(Optional.of(PUBLISHED_AT.plusSeconds(60)), catalogImport.supersededAt());
    }

    @Test
    void aSupersededImportCannotTransitionAgain() {
        CatalogImport catalogImport = publishedImport("a".repeat(64));
        catalogImport.markSuperseded(PUBLISHED_AT.plusSeconds(60));

        assertThrows(
                IllegalStateException.class,
                () -> catalogImport.markSuperseded(PUBLISHED_AT.plusSeconds(120)));
    }

    @Test
    void aRejectedImportIsTerminalAndWasNeverPublished() {
        CatalogImport rejected = CatalogImport.rejected(
                CatalogImportId.from("223e4567-e89b-42d3-a456-426614174001"),
                "academic-institution-catalog/v1",
                metadata("a".repeat(64)),
                3,
                1,
                "catalog_validation_failed",
                PUBLISHED_AT);

        assertEquals(CatalogImport.Status.REJECTED, rejected.status());
        assertEquals(Optional.empty(), rejected.publishedAt());
        assertThrows(IllegalStateException.class, () -> rejected.markSuperseded(PUBLISHED_AT.plusSeconds(60)));
    }

    @Test
    void aSupersessionCannotPredatePublication() {
        CatalogImport catalogImport = publishedImport("a".repeat(64));

        assertThrows(
                IllegalArgumentException.class,
                () -> catalogImport.markSuperseded(PUBLISHED_AT.minusSeconds(1)));
        assertEquals(CatalogImport.Status.PUBLISHED, catalogImport.status());
    }

    @Test
    void publishedCountsMustDescribeTheWholeArtifact() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CatalogImport.published(
                        CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000"),
                        "academic-institution-catalog/v1",
                        metadata("a".repeat(64)),
                        3,
                        2,
                        0,
                        PUBLISHED_AT,
                        Optional.empty()));
    }

    @Test
    void previousPublishedImportCannotReferenceTheImportItself() {
        CatalogImportId id = CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000");

        assertThrows(
                IllegalArgumentException.class,
                () -> CatalogImport.published(
                        id,
                        "academic-institution-catalog/v1",
                        metadata("a".repeat(64)),
                        3,
                        2,
                        1,
                        PUBLISHED_AT,
                        Optional.of(id)));
    }

    @Test
    void previousPublishedImportMayBeAbsentOrReferenceAnotherImport() {
        CatalogImport withoutPrevious = publishedImport("a".repeat(64));
        CatalogImportId previousId = CatalogImportId.from("223e4567-e89b-42d3-a456-426614174001");
        CatalogImport withPrevious = CatalogImport.published(
                CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000"),
                "academic-institution-catalog/v1",
                metadata("b".repeat(64)),
                3,
                2,
                1,
                PUBLISHED_AT,
                Optional.of(previousId));

        assertEquals(Optional.empty(), withoutPrevious.previousPublishedImportId());
        assertEquals(Optional.of(previousId), withPrevious.previousPublishedImportId());
    }

    private static CatalogImport publishedImport(String hash) {
        return CatalogImport.published(
                CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000"),
                "academic-institution-catalog/v1",
                metadata(hash),
                3,
                2,
                1,
                PUBLISHED_AT,
                Optional.empty());
    }

    private static CatalogImportMetadata metadata(String hash) {
        return new CatalogImportMetadata(
                URI.create("https://example.test/ruct"),
                Instant.parse("2026-09-21T09:00:00Z"),
                "Origen de los datos: RUCT",
                new CatalogArtifactHash(hash),
                Optional.empty());
    }
}
