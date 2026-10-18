package com.propractix.academicinstitution.adapter.out.persistence;

import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImport;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CatalogImportMetadata;
import java.net.URI;
import java.util.Optional;

final class CatalogImportPersistenceMapper {

    CatalogImport toDomain(CatalogImportJpaEntity entity) {
        CatalogImportMetadata metadata = new CatalogImportMetadata(
                URI.create(entity.sourceUrl()),
                entity.retrievedAt(),
                entity.sourceAttribution(),
                new CatalogArtifactHash(entity.artifactHash()),
                Optional.ofNullable(entity.sourceDownloadHash()).map(CatalogArtifactHash::new));
        CatalogImportId id = new CatalogImportId(entity.catalogImportId());
        Optional<CatalogImportId> previous =
                Optional.ofNullable(entity.previousPublishedImportId()).map(CatalogImportId::new);

        return switch (CatalogImport.Status.valueOf(entity.status())) {
            case PUBLISHED -> CatalogImport.published(
                    id,
                    entity.schemaVersion(),
                    metadata,
                    entity.totalRowCount(),
                    entity.publishedRowCount(),
                    entity.omittedRowCount(),
                    entity.publishedAt(),
                    previous);
            case SUPERSEDED -> {
                CatalogImport catalogImport = CatalogImport.published(
                        id,
                        entity.schemaVersion(),
                        metadata,
                        entity.totalRowCount(),
                        entity.publishedRowCount(),
                        entity.omittedRowCount(),
                        entity.publishedAt(),
                        previous);
                catalogImport.markSuperseded(entity.supersededAt());
                yield catalogImport;
            }
            case REJECTED -> CatalogImport.rejected(
                    id,
                    entity.schemaVersion(),
                    metadata,
                    entity.totalRowCount(),
                    entity.omittedRowCount(),
                    entity.failureCode(),
                    entity.createdAt());
        };
    }

    CatalogImportJpaEntity toEntity(CatalogImport catalogImport) {
        CatalogImportMetadata metadata = catalogImport.metadata();
        return new CatalogImportJpaEntity(
                catalogImport.id().value(),
                catalogImport.schemaVersion(),
                metadata.sourceUrl().toString(),
                metadata.retrievedAt(),
                metadata.sourceAttribution(),
                metadata.artifactHash().value(),
                metadata.sourceDownloadHash().map(CatalogArtifactHash::value).orElse(null),
                catalogImport.status().name(),
                catalogImport.totalRowCount(),
                catalogImport.publishedRowCount(),
                catalogImport.omittedRowCount(),
                catalogImport.failureCode().orElse(null),
                catalogImport.createdAt(),
                catalogImport.publishedAt().orElse(null),
                catalogImport.supersededAt().orElse(null),
                catalogImport.previousPublishedImportId().map(CatalogImportId::value).orElse(null),
                0);
    }

    void updateEntity(CatalogImportJpaEntity entity, CatalogImport catalogImport) {
        if (catalogImport.status() == CatalogImport.Status.SUPERSEDED) {
            entity.markSuperseded(catalogImport.supersededAt().orElseThrow());
        }
    }
}
