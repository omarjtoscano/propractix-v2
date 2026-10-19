package com.propractix.academicinstitution.adapter.in.commandline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.propractix.academicinstitution.adapter.out.artifact.StrictCsvCatalogArtifactParser;
import com.propractix.academicinstitution.application.command.PublishInstitutionCatalogCommand;
import com.propractix.academicinstitution.application.port.in.CatalogPublicationResult;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.service.PublishInstitutionCatalogService;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class CatalogImportCommandLineAdapterTest {

    private static final String SENSITIVE_NAME = "Universidad Secreta de Prueba";
    private static final String SENSITIVE_REFERENCE = "PRIVATE-0007";
    private static final String SENSITIVE_URL = "https://user:credential@example.invalid/private.csv";
    private static final String SENSITIVE_ATTRIBUTION = "Private source attribution";
    private static final String SENSITIVE_SQL = "SELECT secret FROM credentials";
    private static final byte[] VALID_ARTIFACT = ("sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus\n"
                    + "SYNTHETIC," + SENSITIVE_REFERENCE + "," + SENSITIVE_NAME + ",ES,\n")
            .getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path temporaryDirectory;

    @Test
    void dryRunOnlyReportsCodesHashesAndAggregateCountsAndNeverCallsPublication() throws IOException {
        Path artifact = write("catalog.csv", VALID_ARTIFACT);
        AtomicInteger publicationCalls = new AtomicInteger();
        PublishInstitutionCatalog forbiddenPublisher = command -> {
            publicationCalls.incrementAndGet();
            throw new AssertionError("dry run must not call the publication use case");
        };
        CatalogImportCommandLineAdapter adapter = adapter(artifact, hashOf(VALID_ARTIFACT), forbiddenPublisher);

        CapturedExecution captured = capture(adapter, CatalogImportCommandLineAdapter.Operation.DRY_RUN);

        assertEquals(0, captured.exitCode());
        assertEquals(0, publicationCalls.get());
        assertTrue(captured.output().contains("code=catalog_artifact_valid"));
        assertTrue(captured.output().contains("total_rows=1 published_rows=1 omitted_rows=0"));
        assertSanitized(captured);
    }

    @Test
    void headerOnlyIsRejectedInBothModesWithoutPublicationResources() throws IOException {
        byte[] headerOnly = "sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus\n"
                .getBytes(StandardCharsets.UTF_8);
        Path artifact = write("header-only.csv", headerOnly);
        var parser = new StrictCsvCatalogArtifactParser(Set.of("SYNTHETIC"), Set.of("ES"));
        PublishInstitutionCatalog publisher = new PublishInstitutionCatalogService(
                parser,
                (request, resources) -> {
                    throw new AssertionError("invalid artifact must not access persistence");
                },
                () -> {
                    throw new AssertionError("invalid artifact must not use the clock");
                },
                () -> {
                    throw new AssertionError("invalid artifact must not consume institution UUIDs");
                },
                () -> {
                    throw new AssertionError("invalid artifact must not consume import UUIDs");
                });
        var adapter = adapter(artifact, hashOf(headerOnly), publisher);

        CapturedExecution dryRun = capture(adapter, CatalogImportCommandLineAdapter.Operation.DRY_RUN);
        CapturedExecution publication = capture(adapter, CatalogImportCommandLineAdapter.Operation.PUBLISH);

        assertEquals(4, dryRun.exitCode());
        assertTrue(dryRun.output().contains("code=catalog_artifact_rejected reason=invalid_schema"));
        assertEquals(6, publication.exitCode());
        assertTrue(publication.output().contains("code=catalog_publication_rejected reason=artifact_invalid"));
        assertSanitized(dryRun);
        assertSanitized(publication);
    }

    @Test
    void hashMismatchStopsBeforeParsingOrPublishingAndReturnsNonZero() throws IOException {
        Path artifact = write("catalog.csv", VALID_ARTIFACT);
        CatalogImportCommandLineAdapter adapter = new CatalogImportCommandLineAdapter(
                definition(artifact, new CatalogArtifactHash("0".repeat(64))),
                bytes -> {
                    throw new AssertionError("hash mismatch must precede parsing");
                },
                Optional.of(command -> {
                    throw new AssertionError("hash mismatch must precede publication");
                }));

        CapturedExecution captured = capture(adapter, CatalogImportCommandLineAdapter.Operation.PUBLISH);

        assertEquals(4, captured.exitCode());
        assertTrue(captured.output().contains("code=catalog_artifact_hash_mismatch"));
        assertSanitized(captured);
    }

    @Test
    void publicationDelegatesToE2AndReturnsSanitizedNonZeroCodesForConcurrencyAndFailure()
            throws IOException {
        Path artifact = write("catalog.csv", VALID_ARTIFACT);
        AtomicInteger publicationCalls = new AtomicInteger();
        CatalogImportCommandLineAdapter concurrent = adapter(artifact, hashOf(VALID_ARTIFACT), command -> {
            publicationCalls.incrementAndGet();
            return new CatalogPublicationResult.PublicationInProgress();
        });

        CapturedExecution inProgress = capture(concurrent, CatalogImportCommandLineAdapter.Operation.PUBLISH);

        assertEquals(5, inProgress.exitCode());
        assertEquals(1, publicationCalls.get());
        assertTrue(inProgress.output().contains("code=catalog_publication_in_progress"));
        assertSanitized(inProgress);

        CatalogImportCommandLineAdapter rejected = adapter(
                artifact,
                hashOf(VALID_ARTIFACT),
                command -> new CatalogPublicationResult.Rejected(
                        CatalogPublicationResult.Failure.PUBLICATION_REJECTED));
        CapturedExecution failure = capture(rejected, CatalogImportCommandLineAdapter.Operation.PUBLISH);

        assertEquals(6, failure.exitCode());
        assertTrue(failure.output().contains("code=catalog_publication_rejected"));
        assertSanitized(failure);
    }

    @Test
    void publicationPreservesArtifactAndDownloadHashesAndAllConfiguredMetadata() throws IOException {
        Path artifact = write("catalog.csv", VALID_ARTIFACT);
        CatalogArtifactHash artifactHash = hashOf(VALID_ARTIFACT);
        CatalogArtifactHash sourceDownloadHash = new CatalogArtifactHash("b".repeat(64));
        AtomicReference<PublishInstitutionCatalogCommand> observed = new AtomicReference<>();
        CatalogImportCommandLineDefinition definition = new CatalogImportCommandLineDefinition(
                true,
                artifact,
                "academic-institution-catalog/v1",
                URI.create(SENSITIVE_URL),
                Instant.parse("2026-09-21T00:00:00Z"),
                SENSITIVE_ATTRIBUTION,
                artifactHash,
                Optional.of(sourceDownloadHash));
        CatalogImportCommandLineAdapter adapter = new CatalogImportCommandLineAdapter(
                definition,
                new StrictCsvCatalogArtifactParser(Set.of("SYNTHETIC"), Set.of("ES")),
                Optional.of(command -> {
                    observed.set(command);
                    return new CatalogPublicationResult.Rejected(
                            CatalogPublicationResult.Failure.PUBLICATION_REJECTED);
                }));

        CapturedExecution captured = capture(adapter, CatalogImportCommandLineAdapter.Operation.PUBLISH);

        assertEquals(6, captured.exitCode());
        assertEquals(artifactHash, observed.get().declaredArtifactHash());
        assertEquals(Optional.of(sourceDownloadHash), observed.get().sourceDownloadHash());
        assertEquals(URI.create(SENSITIVE_URL), observed.get().sourceUrl());
        assertEquals(SENSITIVE_ATTRIBUTION, observed.get().sourceAttribution());
        assertSanitized(captured);
    }

    @Test
    void unexpectedStorageFailureReturnsOnlyASanitizedNonZeroResult() throws IOException {
        Path artifact = write("catalog.csv", VALID_ARTIFACT);
        CatalogImportCommandLineAdapter adapter = adapter(artifact, hashOf(VALID_ARTIFACT), command -> {
            throw new IllegalStateException(SENSITIVE_SQL);
        });

        CapturedExecution captured = capture(adapter, CatalogImportCommandLineAdapter.Operation.PUBLISH);

        assertEquals(6, captured.exitCode());
        assertTrue(captured.output().contains("code=catalog_publication_failed"));
        assertSanitized(captured);
    }

    @Test
    void disabledConfigurationDoesNotReadOrPublish() {
        CatalogImportCommandLineDefinition disabled = new CatalogImportCommandLineDefinition(
                false, null, null, null, null, null, null, Optional.empty());
        CatalogImportCommandLineAdapter adapter = new CatalogImportCommandLineAdapter(
                disabled,
                bytes -> {
                    throw new AssertionError("disabled import must not parse");
                },
                Optional.of(command -> {
                    throw new AssertionError("disabled import must not publish");
                }));

        CapturedExecution captured = capture(adapter, CatalogImportCommandLineAdapter.Operation.PUBLISH);

        assertEquals(3, captured.exitCode());
        assertTrue(captured.output().contains("code=catalog_import_disabled"));
    }

    private CatalogImportCommandLineAdapter adapter(
            Path artifact, CatalogArtifactHash hash, PublishInstitutionCatalog publisher) {
        return new CatalogImportCommandLineAdapter(
                definition(artifact, hash),
                new StrictCsvCatalogArtifactParser(Set.of("SYNTHETIC"), Set.of("ES")),
                Optional.of(publisher));
    }

    private static CatalogImportCommandLineDefinition definition(Path artifact, CatalogArtifactHash hash) {
        return new CatalogImportCommandLineDefinition(
                true,
                artifact,
                "academic-institution-catalog/v1",
                URI.create("https://example.invalid/catalog.csv"),
                Instant.parse("2026-09-21T00:00:00Z"),
                "Synthetic test attribution",
                hash,
                Optional.empty());
    }

    private Path write(String name, byte[] bytes) throws IOException {
        Path path = temporaryDirectory.resolve(name);
        Files.write(path, bytes);
        return path;
    }

    private static CapturedExecution capture(
            CatalogImportCommandLineAdapter adapter, CatalogImportCommandLineAdapter.Operation operation) {
        Logger logger = (Logger) LoggerFactory.getLogger(CatalogImportCommandLineAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            int exitCode = adapter.execute(operation, output);
            return new CapturedExecution(
                    exitCode,
                    bytes.toString(StandardCharsets.UTF_8),
                    appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static void assertSanitized(CapturedExecution captured) {
        String observable = captured.output() + String.join("", captured.logs());
        assertFalse(observable.contains(SENSITIVE_NAME));
        assertFalse(observable.contains(SENSITIVE_REFERENCE));
        assertFalse(observable.contains(SENSITIVE_URL));
        assertFalse(observable.contains(SENSITIVE_ATTRIBUTION));
        assertFalse(observable.contains(SENSITIVE_SQL));
        assertFalse(observable.contains("credential"));
        assertFalse(observable.contains(new String(VALID_ARTIFACT, StandardCharsets.UTF_8)));
        assertTrue(captured.logs().stream().allMatch(log -> log.startsWith("catalog_import_result code=")));
    }

    private static CatalogArtifactHash hashOf(byte[] value) {
        try {
            return new CatalogArtifactHash(
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private record CapturedExecution(int exitCode, String output, List<String> logs) {}
}
