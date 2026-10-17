package com.propractix.academicinstitution.application.port.out;

import com.propractix.academicinstitution.domain.event.InstitutionCatalogPublished;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CatalogImportMetadata;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public interface CatalogPublicationRepository {

    /**
     * Performs the complete publication decision and mutation as one transactional repository operation.
     * Implementations must acquire the non-blocking catalog lock before comparing the active artifact, reconcile
     * external references under that lock, call the supplied generators only for a changed snapshot and genuinely
     * new institutions, withdraw absent institutions, replace the active snapshot, and record the domain event before
     * committing. Implementations must not split the active-snapshot comparison and publication into independent
     * repository calls.
     */
    PublicationOutcome publishAtomically(PublicationRequest request, PublicationResources resources);

    record PublicationRequest(
            String schemaVersion,
            CatalogImportMetadata metadata,
            List<CatalogInstitutionCandidate> candidates,
            int totalRowCount,
            int omittedRowCount) {

        public PublicationRequest {
            Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
            if (schemaVersion.isBlank()) {
                throw new IllegalArgumentException("schemaVersion must not be blank");
            }
            Objects.requireNonNull(metadata, "metadata must not be null");
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates must not be null"));
            if (totalRowCount < 0 || omittedRowCount < 0) {
                throw new IllegalArgumentException("row counts must not be negative");
            }
            if ((long) candidates.size() + omittedRowCount != totalRowCount) {
                throw new IllegalArgumentException("candidate and omitted counts must equal totalRowCount");
            }
        }
    }

    interface PublicationResources {

        AcademicInstitutionId nextAcademicInstitutionId();

        CatalogImportId nextCatalogImportId();

        Instant now();
    }

    sealed interface PublicationOutcome
            permits Published, ActiveArtifactUnchanged, PublicationInProgress, Rejected {}

    record Published(PublishedSnapshot snapshot, InstitutionCatalogPublished event) implements PublicationOutcome {

        public Published {
            Objects.requireNonNull(snapshot, "snapshot must not be null");
            Objects.requireNonNull(event, "event must not be null");
            if (!snapshot.catalogImportId().equals(event.catalogImportId())
                    || !snapshot.artifactHash().equals(event.artifactHash())
                    || !snapshot.publishedAt().equals(event.occurredAt())) {
                throw new IllegalArgumentException("snapshot and publication event must describe the same publication");
            }
        }
    }

    record ActiveArtifactUnchanged(PublishedSnapshot snapshot) implements PublicationOutcome {

        public ActiveArtifactUnchanged {
            Objects.requireNonNull(snapshot, "snapshot must not be null");
        }
    }

    record PublicationInProgress() implements PublicationOutcome {}

    record Rejected(Failure failure) implements PublicationOutcome {

        public Rejected {
            Objects.requireNonNull(failure, "failure must not be null");
        }
    }

    record PublishedSnapshot(
            CatalogImportId catalogImportId, CatalogArtifactHash artifactHash, Instant publishedAt) {

        public PublishedSnapshot {
            Objects.requireNonNull(catalogImportId, "catalogImportId must not be null");
            Objects.requireNonNull(artifactHash, "artifactHash must not be null");
            Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        }
    }

    enum Failure {
        RECONCILIATION_CONFLICT,
        INVALID_PUBLICATION,
        STORAGE_FAILURE
    }
}
