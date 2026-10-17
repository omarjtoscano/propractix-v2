package com.propractix.academicinstitution.application.service;

import com.propractix.academicinstitution.application.command.PublishInstitutionCatalogCommand;
import com.propractix.academicinstitution.application.port.in.CatalogPublicationResult;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.port.out.AcademicInstitutionIdGenerator;
import com.propractix.academicinstitution.application.port.out.ApplicationClock;
import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import com.propractix.academicinstitution.application.port.out.CatalogImportIdGenerator;
import com.propractix.academicinstitution.application.port.out.CatalogPublicationRepository;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CatalogImportMetadata;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

public final class PublishInstitutionCatalogService implements PublishInstitutionCatalog {

    private final CatalogArtifactParser artifactParser;
    private final CatalogPublicationRepository publicationRepository;
    private final ApplicationClock clock;
    private final AcademicInstitutionIdGenerator academicInstitutionIdGenerator;
    private final CatalogImportIdGenerator catalogImportIdGenerator;

    public PublishInstitutionCatalogService(
            CatalogArtifactParser artifactParser,
            CatalogPublicationRepository publicationRepository,
            ApplicationClock clock,
            AcademicInstitutionIdGenerator academicInstitutionIdGenerator,
            CatalogImportIdGenerator catalogImportIdGenerator) {
        this.artifactParser = Objects.requireNonNull(artifactParser, "artifactParser must not be null");
        this.publicationRepository =
                Objects.requireNonNull(publicationRepository, "publicationRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.academicInstitutionIdGenerator = Objects.requireNonNull(
                academicInstitutionIdGenerator, "academicInstitutionIdGenerator must not be null");
        this.catalogImportIdGenerator =
                Objects.requireNonNull(catalogImportIdGenerator, "catalogImportIdGenerator must not be null");
    }

    @Override
    public CatalogPublicationResult publish(PublishInstitutionCatalogCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        byte[] exactArtifactBytes = command.artifactBytes();
        CatalogArtifactHash calculatedHash = calculateSha256(exactArtifactBytes);
        if (!calculatedHash.equals(command.declaredArtifactHash())) {
            return new CatalogPublicationResult.Rejected(
                    CatalogPublicationResult.Failure.ARTIFACT_HASH_MISMATCH);
        }

        CatalogArtifactParser.ParseResult parseResult = artifactParser.parse(exactArtifactBytes);
        if (parseResult instanceof CatalogArtifactParser.RejectedArtifact) {
            return new CatalogPublicationResult.Rejected(CatalogPublicationResult.Failure.ARTIFACT_INVALID);
        }
        CatalogArtifactParser.ParsedArtifact parsedArtifact =
                (CatalogArtifactParser.ParsedArtifact) parseResult;

        CatalogImportMetadata metadata = new CatalogImportMetadata(
                command.sourceUrl(),
                command.retrievedAt(),
                command.sourceAttribution(),
                calculatedHash,
                command.sourceDownloadHash());
        CatalogPublicationRepository.PublicationRequest request =
                new CatalogPublicationRepository.PublicationRequest(
                        command.schemaVersion(),
                        metadata,
                        parsedArtifact.candidates(),
                        parsedArtifact.totalRowCount(),
                        parsedArtifact.omittedRowCount());

        CatalogPublicationRepository.PublicationOutcome outcome =
                publicationRepository.publishAtomically(request, publicationResources());
        return toResult(outcome);
    }

    private CatalogPublicationRepository.PublicationResources publicationResources() {
        return new CatalogPublicationRepository.PublicationResources() {
            @Override
            public AcademicInstitutionId nextAcademicInstitutionId() {
                return academicInstitutionIdGenerator.nextId();
            }

            @Override
            public CatalogImportId nextCatalogImportId() {
                return catalogImportIdGenerator.nextId();
            }

            @Override
            public Instant now() {
                return clock.now();
            }
        };
    }

    private static CatalogPublicationResult toResult(
            CatalogPublicationRepository.PublicationOutcome outcome) {
        Objects.requireNonNull(outcome, "publication outcome must not be null");
        if (outcome instanceof CatalogPublicationRepository.Published published) {
            CatalogPublicationRepository.PublishedSnapshot snapshot = published.snapshot();
            return new CatalogPublicationResult.Published(
                    snapshot.catalogImportId(), snapshot.artifactHash(), snapshot.publishedAt(), published.event());
        }
        if (outcome instanceof CatalogPublicationRepository.ActiveArtifactUnchanged unchanged) {
            CatalogPublicationRepository.PublishedSnapshot snapshot = unchanged.snapshot();
            return new CatalogPublicationResult.ActiveArtifactUnchanged(
                    snapshot.catalogImportId(), snapshot.artifactHash(), snapshot.publishedAt());
        }
        if (outcome instanceof CatalogPublicationRepository.PublicationInProgress) {
            return new CatalogPublicationResult.PublicationInProgress();
        }
        if (outcome instanceof CatalogPublicationRepository.Rejected) {
            return new CatalogPublicationResult.Rejected(CatalogPublicationResult.Failure.PUBLICATION_REJECTED);
        }
        throw new IllegalStateException("unsupported publication outcome");
    }

    private static CatalogArtifactHash calculateSha256(byte[] exactArtifactBytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(exactArtifactBytes);
            return new CatalogArtifactHash(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
