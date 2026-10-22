package com.propractix.academicinstitution.adapter.in.commandline;

import com.propractix.academicinstitution.application.command.PublishInstitutionCatalogCommand;
import com.propractix.academicinstitution.application.port.in.CatalogPublicationResult;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CatalogImportCommandLineAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogImportCommandLineAdapter.class);

    private final CatalogImportCommandLineDefinition definition;
    private final CatalogArtifactParser parser;
    private final Optional<PublishInstitutionCatalog> publisher;

    public CatalogImportCommandLineAdapter(
            CatalogImportCommandLineDefinition definition,
            CatalogArtifactParser parser,
            Optional<PublishInstitutionCatalog> publisher) {
        this.definition = Objects.requireNonNull(definition, "definition must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    public int execute(Operation operation, PrintStream output) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(output, "output must not be null");
        if (!definition.enabled()) {
            return emit(
                    output,
                    ExitCode.CONFIGURATION_REJECTED,
                    "code=catalog_import_disabled",
                    operation == Operation.PUBLISH);
        }

        byte[] exactArtifactBytes;
        try {
            exactArtifactBytes = Files.readAllBytes(definition.artifactPath());
        } catch (IOException exception) {
            return emit(
                    output,
                    ExitCode.INPUT_UNAVAILABLE,
                    "code=catalog_artifact_unavailable",
                    operation == Operation.PUBLISH);
        }

        CatalogArtifactHash calculatedHash = calculateSha256(exactArtifactBytes);
        if (!calculatedHash.equals(definition.artifactHash())) {
            return emit(
                    output,
                    ExitCode.ARTIFACT_REJECTED,
                    "code=catalog_artifact_hash_mismatch artifact_hash=" + calculatedHash.value(),
                    operation == Operation.PUBLISH);
        }

        if (operation == Operation.DRY_RUN) {
            return dryRun(exactArtifactBytes, calculatedHash, output);
        }
        return publish(exactArtifactBytes, output);
    }

    private int dryRun(byte[] exactArtifactBytes, CatalogArtifactHash calculatedHash, PrintStream output) {
        CatalogArtifactParser.ParseResult result = parser.parse(exactArtifactBytes);
        if (result instanceof CatalogArtifactParser.RejectedArtifact rejected) {
            return emit(
                    output,
                    ExitCode.ARTIFACT_REJECTED,
                    "code=catalog_artifact_rejected reason=" + code(rejected.failure())
                            + " total_rows=0 published_rows=0 omitted_rows=0 artifact_hash="
                            + calculatedHash.value(),
                    false);
        }
        CatalogArtifactParser.ParsedArtifact parsed = (CatalogArtifactParser.ParsedArtifact) result;
        return emit(
                output,
                ExitCode.SUCCESS,
                "code=catalog_artifact_valid total_rows=" + parsed.totalRowCount()
                        + " published_rows=" + parsed.candidates().size()
                        + " omitted_rows=" + parsed.omittedRowCount()
                        + " artifact_hash=" + calculatedHash.value(),
                false);
    }

    private int publish(byte[] exactArtifactBytes, PrintStream output) {
        PublishInstitutionCatalog useCase = publisher.orElse(null);
        if (useCase == null) {
            return emit(
                    output, ExitCode.CONFIGURATION_REJECTED, "code=catalog_publication_unavailable", true);
        }
        PublishInstitutionCatalogCommand command = new PublishInstitutionCatalogCommand(
                exactArtifactBytes,
                definition.schemaVersion(),
                definition.sourceUrl(),
                definition.retrievedAt(),
                definition.sourceAttribution(),
                definition.artifactHash(),
                definition.sourceDownloadHash());
        CatalogPublicationResult result;
        try {
            result = useCase.publish(command);
        } catch (RuntimeException exception) {
            return emit(output, ExitCode.PUBLICATION_REJECTED, "code=catalog_publication_failed", true);
        }
        if (result instanceof CatalogPublicationResult.Published published) {
            return emit(
                    output,
                    ExitCode.SUCCESS,
                    "code=catalog_published import_id=" + published.catalogImportId().value()
                            + " artifact_hash=" + published.artifactHash().value(),
                    true);
        }
        if (result instanceof CatalogPublicationResult.ActiveArtifactUnchanged unchanged) {
            return emit(
                    output,
                    ExitCode.SUCCESS,
                    "code=catalog_active_artifact_unchanged import_id="
                            + unchanged.catalogImportId().value() + " artifact_hash="
                            + unchanged.artifactHash().value(),
                    true);
        }
        if (result instanceof CatalogPublicationResult.PublicationInProgress) {
            return emit(
                    output, ExitCode.CONCURRENT_PUBLICATION, "code=catalog_publication_in_progress", true);
        }
        CatalogPublicationResult.Rejected rejected = (CatalogPublicationResult.Rejected) result;
        return emit(
                output,
                ExitCode.PUBLICATION_REJECTED,
                "code=catalog_publication_rejected reason=" + code(rejected.failure()),
                true);
    }

    private static int emit(PrintStream output, ExitCode exitCode, String result, boolean logResult) {
        String sanitized = "catalog_import_result " + result;
        output.println(sanitized);
        if (logResult) {
            LOGGER.info("{}", sanitized);
        }
        return exitCode.value;
    }

    private static String code(Enum<?> value) {
        return value.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static CatalogArtifactHash calculateSha256(byte[] exactArtifactBytes) {
        try {
            return new CatalogArtifactHash(HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(exactArtifactBytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public enum Operation {
        DRY_RUN,
        PUBLISH
    }

    private enum ExitCode {
        SUCCESS(0),
        INPUT_UNAVAILABLE(2),
        CONFIGURATION_REJECTED(3),
        ARTIFACT_REJECTED(4),
        CONCURRENT_PUBLICATION(5),
        PUBLICATION_REJECTED(6);

        private final int value;

        ExitCode(int value) {
            this.value = value;
        }
    }
}
