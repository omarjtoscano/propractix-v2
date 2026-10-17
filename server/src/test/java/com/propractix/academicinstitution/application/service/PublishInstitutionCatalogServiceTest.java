package com.propractix.academicinstitution.application.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.propractix.academicinstitution.application.command.PublishInstitutionCatalogCommand;
import com.propractix.academicinstitution.application.port.in.CatalogPublicationResult;
import com.propractix.academicinstitution.application.port.out.AcademicInstitutionIdGenerator;
import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import com.propractix.academicinstitution.application.port.out.CatalogImportIdGenerator;
import com.propractix.academicinstitution.application.port.out.CatalogInstitutionCandidate;
import com.propractix.academicinstitution.application.port.out.CatalogPublicationRepository;
import com.propractix.academicinstitution.domain.event.InstitutionCatalogPublished;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.ExternalInstitutionReference;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublishInstitutionCatalogServiceTest {

    private static final Instant RETRIEVED_AT = Instant.parse("2026-10-16T09:15:00Z");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-17T10:30:00Z");
    private static final URI SOURCE_URL = URI.create("https://example.test/catalog.csv");
    private static final String SOURCE_ATTRIBUTION = "Exact source attribution";
    private static final byte[] ARTIFACT_A = "sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus\n"
            .concat("SOURCE,0001,Universidad Alfa,ES,\n")
            .getBytes(StandardCharsets.UTF_8);
    private static final byte[] ARTIFACT_B = "sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus\n"
            .concat("SOURCE,0001,Universidad Alfa Renovada,ES,ACTIVE\n")
            .concat("SOURCE,0002,Universidad Beta,ES,\n")
            .getBytes(StandardCharsets.UTF_8);

    private CapturingParser parser;
    private AtomicFakePublicationRepository repository;
    private SequenceAcademicInstitutionIdGenerator institutionIds;
    private SequenceCatalogImportIdGenerator importIds;
    private PublishInstitutionCatalogService service;

    @BeforeEach
    void setUp() {
        parser = new CapturingParser();
        repository = new AtomicFakePublicationRepository();
        institutionIds = new SequenceAcademicInstitutionIdGenerator(
                institutionId("123e4567-e89b-42d3-a456-426614174000"),
                institutionId("123e4567-e89b-42d3-a456-426614174001"),
                institutionId("123e4567-e89b-42d3-a456-426614174002"));
        importIds = new SequenceCatalogImportIdGenerator(
                importId("223e4567-e89b-42d3-a456-426614174000"),
                importId("223e4567-e89b-42d3-a456-426614174001"),
                importId("223e4567-e89b-42d3-a456-426614174002"));
        service = new PublishInstitutionCatalogService(
                parser, repository, () -> PUBLISHED_AT, institutionIds, importIds);
    }

    @Test
    void verifiesHashBeforeParsingAndRejectsMismatchWithoutCallingRepository() {
        PublishInstitutionCatalogCommand command = command(ARTIFACT_A, new CatalogArtifactHash("0".repeat(64)));

        CatalogPublicationResult result = service.publish(command);

        CatalogPublicationResult.Rejected rejected =
                assertInstanceOf(CatalogPublicationResult.Rejected.class, result);
        assertEquals(CatalogPublicationResult.Failure.ARTIFACT_HASH_MISMATCH, rejected.failure());
        assertEquals(0, parser.calls);
        assertEquals(0, repository.calls);
        assertEquals(0, institutionIds.calls());
        assertEquals(0, importIds.calls());
    }

    @Test
    void passesExactBytesToParserAndPropagatesMetadataWithoutAlteration() {
        CatalogArtifactHash sourceDownloadHash = new CatalogArtifactHash("d".repeat(64));
        parser.result = parsed(candidate("0001", "Universidad Alfa", Optional.empty()));
        PublishInstitutionCatalogCommand command = command(ARTIFACT_A, hashOf(ARTIFACT_A), sourceDownloadHash);

        CatalogPublicationResult result = service.publish(command);

        assertInstanceOf(CatalogPublicationResult.Published.class, result);
        assertArrayEquals(ARTIFACT_A, parser.receivedBytes);
        CatalogPublicationRepository.PublicationRequest request = repository.lastRequest;
        assertEquals("academic-institution-catalog/v1", request.schemaVersion());
        assertEquals(SOURCE_URL, request.metadata().sourceUrl());
        assertEquals(RETRIEVED_AT, request.metadata().retrievedAt());
        assertEquals(SOURCE_ATTRIBUTION, request.metadata().sourceAttribution());
        assertEquals(hashOf(ARTIFACT_A), request.metadata().artifactHash());
        assertEquals(Optional.of(sourceDownloadHash), request.metadata().sourceDownloadHash());
        assertEquals(Optional.empty(), request.candidates().getFirst().sourceStatus());
        assertThrows(UnsupportedOperationException.class, () -> request.candidates().clear());
    }

    @Test
    void publishesNewSnapshotWithControlledClockAndGeneratorsAndOneEvent() {
        parser.result = parsed(candidate("0001", "Universidad Alfa", Optional.empty()));

        CatalogPublicationResult.Published published = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(ARTIFACT_A, hashOf(ARTIFACT_A))));

        assertEquals(importId("223e4567-e89b-42d3-a456-426614174000"), published.catalogImportId());
        assertEquals(PUBLISHED_AT, published.publishedAt());
        assertEquals(published.catalogImportId(), published.event().catalogImportId());
        assertEquals(Optional.empty(), published.event().previousPublishedImportId());
        assertEquals(1, institutionIds.calls());
        assertEquals(1, importIds.calls());
        assertEquals(1, repository.eventCount);
    }

    @Test
    void repeatingActiveArtifactIsCompleteNoOpAndDoesNotConsumeIdsOrClock() {
        parser.result = parsed(candidate("0001", "Universidad Alfa", Optional.empty()));
        CatalogPublicationResult.Published first = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(ARTIFACT_A, hashOf(ARTIFACT_A))));
        int clockCallsAfterFirstPublication = repository.clockCalls;

        CatalogPublicationResult.ActiveArtifactUnchanged unchanged = assertInstanceOf(
                CatalogPublicationResult.ActiveArtifactUnchanged.class,
                service.publish(command(ARTIFACT_A, hashOf(ARTIFACT_A))));

        assertEquals(first.catalogImportId(), unchanged.catalogImportId());
        assertEquals(1, repository.importCount);
        assertEquals(1, repository.eventCount);
        assertEquals(1, institutionIds.calls());
        assertEquals(1, importIds.calls());
        assertEquals(clockCallsAfterFirstPublication, repository.clockCalls);
    }

    @Test
    void republishingSupersededArtifactCreatesImportAndEventWhileReusingKnownIdentity() {
        parser.result = parsed(candidate("0001", "Universidad Alfa", Optional.empty()));
        CatalogPublicationResult.Published first = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(ARTIFACT_A, hashOf(ARTIFACT_A))));
        AcademicInstitutionId knownId = repository.identities.get(reference("0001"));

        parser.result = parsed(
                candidate("0001", "Universidad Alfa Renovada", Optional.of("ACTIVE")),
                candidate("0002", "Universidad Beta", Optional.empty()));
        CatalogPublicationResult.Published second = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(ARTIFACT_B, hashOf(ARTIFACT_B))));

        parser.result = parsed(candidate("0001", "Universidad Alfa", Optional.empty()));
        CatalogPublicationResult.Published republished = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(ARTIFACT_A, hashOf(ARTIFACT_A))));

        assertNotEquals(first.catalogImportId(), republished.catalogImportId());
        assertEquals(second.catalogImportId(), republished.event().previousPublishedImportId().orElseThrow());
        assertEquals(knownId, repository.identities.get(reference("0001")));
        assertEquals(2, institutionIds.calls(), "only the two distinct references consume IDs");
        assertEquals(3, importIds.calls());
        assertEquals(3, repository.eventCount);
        assertEquals(Optional.empty(), repository.statuses.get(reference("0001")));
    }

    @Test
    void returnsStableInProgressResultWithoutChangingCurrentSnapshot() {
        parser.result = parsed(candidate("0001", "Universidad Alfa", Optional.empty()));
        CatalogPublicationResult.Published first = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(ARTIFACT_A, hashOf(ARTIFACT_A))));
        repository.inProgress = true;

        CatalogPublicationResult result = service.publish(command(ARTIFACT_B, hashOf(ARTIFACT_B)));

        assertInstanceOf(CatalogPublicationResult.PublicationInProgress.class, result);
        assertEquals(first.catalogImportId(), repository.activeSnapshot.catalogImportId());
        assertEquals(1, repository.importCount);
        assertEquals(1, repository.eventCount);
    }

    @Test
    void typedRepositoryRejectionDoesNotReplaceCurrentSnapshot() {
        parser.result = parsed(candidate("0001", "Universidad Alfa", Optional.empty()));
        CatalogPublicationResult.Published first = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(ARTIFACT_A, hashOf(ARTIFACT_A))));
        repository.reject = true;

        CatalogPublicationResult.Rejected rejected = assertInstanceOf(
                CatalogPublicationResult.Rejected.class,
                service.publish(command(ARTIFACT_B, hashOf(ARTIFACT_B))));

        assertEquals(CatalogPublicationResult.Failure.PUBLICATION_REJECTED, rejected.failure());
        assertEquals(first.catalogImportId(), repository.activeSnapshot.catalogImportId());
        assertEquals(1, repository.importCount);
        assertEquals(1, repository.eventCount);
    }

    @Test
    void parserRejectionIsTypedAndDoesNotExposeArtifactOrReferences() {
        parser.result = new CatalogArtifactParser.RejectedArtifact(CatalogArtifactParser.Failure.INVALID_CONTENT);
        String sensitiveReference = "SOURCE-SECRET-0001";
        byte[] invalidArtifact = sensitiveReference.getBytes(StandardCharsets.UTF_8);

        CatalogPublicationResult.Rejected rejected = assertInstanceOf(
                CatalogPublicationResult.Rejected.class,
                service.publish(command(invalidArtifact, hashOf(invalidArtifact))));

        assertEquals(CatalogPublicationResult.Failure.ARTIFACT_INVALID, rejected.failure());
        assertFalse(rejected.toString().contains(sensitiveReference));
        assertFalse(rejected.toString().contains(new String(invalidArtifact, StandardCharsets.UTF_8)));
        assertEquals(0, repository.calls);
    }

    private static CatalogArtifactParser.ParsedArtifact parsed(CatalogInstitutionCandidate... candidates) {
        return new CatalogArtifactParser.ParsedArtifact(List.of(candidates), candidates.length, 0);
    }

    private static CatalogInstitutionCandidate candidate(
            String sourceRecordId, String officialName, Optional<String> sourceStatus) {
        return new CatalogInstitutionCandidate(
                reference(sourceRecordId),
                new OfficialInstitutionName(officialName),
                new CountryCode("ES"),
                sourceStatus);
    }

    private static ExternalInstitutionReference reference(String sourceRecordId) {
        return new ExternalInstitutionReference("SOURCE", sourceRecordId);
    }

    private static PublishInstitutionCatalogCommand command(byte[] bytes, CatalogArtifactHash hash) {
        return command(bytes, hash, null);
    }

    private static PublishInstitutionCatalogCommand command(
            byte[] bytes, CatalogArtifactHash hash, CatalogArtifactHash sourceDownloadHash) {
        return new PublishInstitutionCatalogCommand(
                bytes,
                "academic-institution-catalog/v1",
                SOURCE_URL,
                RETRIEVED_AT,
                SOURCE_ATTRIBUTION,
                hash,
                Optional.ofNullable(sourceDownloadHash));
    }

    private static CatalogArtifactHash hashOf(byte[] bytes) {
        try {
            return new CatalogArtifactHash(
                    java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private static AcademicInstitutionId institutionId(String value) {
        return AcademicInstitutionId.from(value);
    }

    private static CatalogImportId importId(String value) {
        return CatalogImportId.from(value);
    }

    private static final class CapturingParser implements CatalogArtifactParser {

        private int calls;
        private byte[] receivedBytes;
        private ParseResult result = new RejectedArtifact(Failure.INVALID_CONTENT);

        @Override
        public ParseResult parse(byte[] exactArtifactBytes) {
            calls++;
            receivedBytes = exactArtifactBytes.clone();
            return result;
        }
    }

    private static final class SequenceAcademicInstitutionIdGenerator
            implements AcademicInstitutionIdGenerator {

        private final Queue<AcademicInstitutionId> values;
        private final AtomicInteger calls = new AtomicInteger();

        private SequenceAcademicInstitutionIdGenerator(AcademicInstitutionId... values) {
            this.values = new ArrayDeque<>(List.of(values));
        }

        @Override
        public AcademicInstitutionId nextId() {
            calls.incrementAndGet();
            return values.remove();
        }

        private int calls() {
            return calls.get();
        }
    }

    private static final class SequenceCatalogImportIdGenerator implements CatalogImportIdGenerator {

        private final Queue<CatalogImportId> values;
        private final AtomicInteger calls = new AtomicInteger();

        private SequenceCatalogImportIdGenerator(CatalogImportId... values) {
            this.values = new ArrayDeque<>(List.of(values));
        }

        @Override
        public CatalogImportId nextId() {
            calls.incrementAndGet();
            return values.remove();
        }

        private int calls() {
            return calls.get();
        }
    }

    private static final class AtomicFakePublicationRepository implements CatalogPublicationRepository {

        private final Map<ExternalInstitutionReference, AcademicInstitutionId> identities = new HashMap<>();
        private final Map<ExternalInstitutionReference, Optional<String>> statuses = new HashMap<>();
        private final List<InstitutionCatalogPublished> events = new ArrayList<>();
        private int calls;
        private int importCount;
        private int eventCount;
        private int clockCalls;
        private boolean inProgress;
        private boolean reject;
        private PublicationRequest lastRequest;
        private PublishedSnapshot activeSnapshot;

        @Override
        public PublicationOutcome publishAtomically(PublicationRequest request, PublicationResources resources) {
            calls++;
            lastRequest = request;
            if (inProgress) {
                return new PublicationInProgress();
            }
            if (activeSnapshot != null && activeSnapshot.artifactHash().equals(request.metadata().artifactHash())) {
                return new ActiveArtifactUnchanged(activeSnapshot);
            }
            if (reject) {
                return new Rejected(Failure.RECONCILIATION_CONFLICT);
            }

            CatalogImportId previousId = activeSnapshot == null ? null : activeSnapshot.catalogImportId();
            CatalogImportId importId = resources.nextCatalogImportId();
            Instant publishedAt = resources.now();
            clockCalls++;
            for (CatalogInstitutionCandidate candidate : request.candidates()) {
                identities.computeIfAbsent(
                        candidate.externalReference(), ignored -> resources.nextAcademicInstitutionId());
                statuses.put(candidate.externalReference(), candidate.sourceStatus());
            }
            CatalogPublicationRepository.PublishedSnapshot newSnapshot = new PublishedSnapshot(
                    importId, request.metadata().artifactHash(), publishedAt);
            InstitutionCatalogPublished event = new InstitutionCatalogPublished(
                    importId,
                    Optional.ofNullable(previousId),
                    request.metadata().artifactHash(),
                    publishedAt);
            activeSnapshot = newSnapshot;
            importCount++;
            eventCount++;
            events.add(event);
            return new Published(newSnapshot, event);
        }
    }
}
