package com.propractix.academicinstitution.application.port.in;

import com.propractix.academicinstitution.domain.event.InstitutionCatalogPublished;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import java.time.Instant;
import java.util.Objects;

public sealed interface CatalogPublicationResult
        permits CatalogPublicationResult.Published,
                CatalogPublicationResult.ActiveArtifactUnchanged,
                CatalogPublicationResult.PublicationInProgress,
                CatalogPublicationResult.Rejected {

    record Published(
            CatalogImportId catalogImportId,
            CatalogArtifactHash artifactHash,
            Instant publishedAt,
            InstitutionCatalogPublished event)
            implements CatalogPublicationResult {

        public Published {
            Objects.requireNonNull(catalogImportId, "catalogImportId must not be null");
            Objects.requireNonNull(artifactHash, "artifactHash must not be null");
            Objects.requireNonNull(publishedAt, "publishedAt must not be null");
            Objects.requireNonNull(event, "event must not be null");
        }
    }

    record ActiveArtifactUnchanged(
            CatalogImportId catalogImportId, CatalogArtifactHash artifactHash, Instant publishedAt)
            implements CatalogPublicationResult {

        public ActiveArtifactUnchanged {
            Objects.requireNonNull(catalogImportId, "catalogImportId must not be null");
            Objects.requireNonNull(artifactHash, "artifactHash must not be null");
            Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        }
    }

    record PublicationInProgress() implements CatalogPublicationResult {}

    record Rejected(Failure failure) implements CatalogPublicationResult {

        public Rejected {
            Objects.requireNonNull(failure, "failure must not be null");
        }
    }

    enum Failure {
        ARTIFACT_HASH_MISMATCH,
        ARTIFACT_INVALID,
        PUBLICATION_REJECTED
    }
}
