package com.propractix.academicinstitution.domain.event;

import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record InstitutionCatalogPublished(
        CatalogImportId catalogImportId,
        Optional<CatalogImportId> previousPublishedImportId,
        CatalogArtifactHash artifactHash,
        Instant occurredAt) {

    public InstitutionCatalogPublished {
        Objects.requireNonNull(catalogImportId, "catalogImportId must not be null");
        Objects.requireNonNull(previousPublishedImportId, "previousPublishedImportId must not be null");
        Objects.requireNonNull(artifactHash, "artifactHash must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
