package com.propractix.academicinstitution.domain.service;

import static com.propractix.academicinstitution.domain.service.CatalogPublicationPolicy.PublicationDecision.ACTIVE_ARTIFACT_UNCHANGED;
import static com.propractix.academicinstitution.domain.service.CatalogPublicationPolicy.PublicationDecision.PUBLISH_NEW_SNAPSHOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.propractix.academicinstitution.domain.event.InstitutionCatalogPublished;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImport;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CatalogImportMetadata;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CatalogPublicationPolicyTest {

    private static final Instant FIRST_PUBLICATION = Instant.parse("2026-09-21T10:00:00Z");
    private static final Instant SECOND_PUBLICATION = Instant.parse("2026-09-22T10:00:00Z");
    private final CatalogPublicationPolicy policy = new CatalogPublicationPolicy();

    @Test
    void repeatingTheActiveArtifactIsACompleteNoOp() {
        CatalogImport active = publishedImport(
                "223e4567-e89b-42d3-a456-426614174000",
                "a".repeat(64),
                FIRST_PUBLICATION,
                Optional.empty());

        CatalogPublicationPolicy.PublicationDecision decision =
                policy.evaluate(Optional.of(active), active.metadata().artifactHash());

        assertEquals(ACTIVE_ARTIFACT_UNCHANGED, decision);
        assertFalse(decision.createsImport());
        assertFalse(decision.changesSnapshot());
        assertFalse(decision.emitsEvent());
        assertTrue(policy.eventFor(decision, Optional.empty()).isEmpty());
        assertEquals(CatalogImport.Status.PUBLISHED, active.status());
    }

    @Test
    void republishingAnArtifactFoundOnlyInSupersededHistoryCreatesANewPublication() {
        CatalogImport historicalA = publishedImport(
                "223e4567-e89b-42d3-a456-426614174000",
                "a".repeat(64),
                FIRST_PUBLICATION,
                Optional.empty());
        historicalA.markSuperseded(FIRST_PUBLICATION.plusSeconds(60));
        CatalogImport activeB = publishedImport(
                "223e4567-e89b-42d3-a456-426614174001",
                "b".repeat(64),
                FIRST_PUBLICATION.plusSeconds(60),
                Optional.of(historicalA.id()));

        CatalogPublicationPolicy.PublicationDecision decision =
                policy.evaluate(Optional.of(activeB), historicalA.metadata().artifactHash());
        CatalogImport republishedA = publishedImport(
                "223e4567-e89b-42d3-a456-426614174002",
                "a".repeat(64),
                SECOND_PUBLICATION,
                Optional.of(activeB.id()));
        InstitutionCatalogPublished event = policy.eventFor(decision, Optional.of(republishedA)).orElseThrow();

        assertEquals(PUBLISH_NEW_SNAPSHOT, decision);
        assertTrue(decision.createsImport());
        assertTrue(decision.changesSnapshot());
        assertTrue(decision.emitsEvent());
        assertNotEquals(historicalA.id(), republishedA.id());
        assertEquals(activeB.id(), event.previousPublishedImportId().orElseThrow());
        assertEquals(republishedA.id(), event.catalogImportId());
        assertEquals(historicalA.metadata().artifactHash(), event.artifactHash());
    }

    @Test
    void theFirstPublicationChangesTheSnapshotAndProducesAnEvent() {
        CatalogArtifactHash requestedHash = new CatalogArtifactHash("a".repeat(64));
        CatalogPublicationPolicy.PublicationDecision decision = policy.evaluate(Optional.empty(), requestedHash);
        CatalogImport firstImport = publishedImport(
                "223e4567-e89b-42d3-a456-426614174000",
                requestedHash.value(),
                FIRST_PUBLICATION,
                Optional.empty());

        InstitutionCatalogPublished event = policy.eventFor(decision, Optional.of(firstImport)).orElseThrow();

        assertEquals(PUBLISH_NEW_SNAPSHOT, decision);
        assertEquals(firstImport.id(), event.catalogImportId());
        assertEquals(Optional.empty(), event.previousPublishedImportId());
        assertEquals(FIRST_PUBLICATION, event.occurredAt());
    }

    @Test
    void aRejectedPublicationCannotReplaceTheActiveSnapshotOrProduceAnEvent() {
        CatalogImport active = publishedImport(
                "223e4567-e89b-42d3-a456-426614174000",
                "a".repeat(64),
                FIRST_PUBLICATION,
                Optional.empty());
        CatalogImport rejected = CatalogImport.rejected(
                CatalogImportId.from("223e4567-e89b-42d3-a456-426614174001"),
                "academic-institution-catalog/v1",
                metadata("b".repeat(64)),
                3,
                3,
                "catalog_validation_failed",
                SECOND_PUBLICATION);

        assertThrows(
                IllegalArgumentException.class,
                () -> policy.eventFor(PUBLISH_NEW_SNAPSHOT, Optional.of(rejected)));
        assertEquals(CatalogImport.Status.PUBLISHED, active.status());
        assertEquals(CatalogImport.Status.REJECTED, rejected.status());
    }

    @Test
    void onlyAPublishedImportCanBePresentedAsTheCurrentSnapshot() {
        CatalogImport superseded = publishedImport(
                "223e4567-e89b-42d3-a456-426614174000",
                "a".repeat(64),
                FIRST_PUBLICATION,
                Optional.empty());
        superseded.markSuperseded(FIRST_PUBLICATION.plusSeconds(60));

        assertThrows(
                IllegalArgumentException.class,
                () -> policy.evaluate(Optional.of(superseded), new CatalogArtifactHash("b".repeat(64))));
    }

    private static CatalogImport publishedImport(
            String id,
            String hash,
            Instant publishedAt,
            Optional<CatalogImportId> previousPublishedImportId) {
        return CatalogImport.published(
                CatalogImportId.from(id),
                "academic-institution-catalog/v1",
                metadata(hash),
                3,
                2,
                1,
                publishedAt,
                previousPublishedImportId);
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
