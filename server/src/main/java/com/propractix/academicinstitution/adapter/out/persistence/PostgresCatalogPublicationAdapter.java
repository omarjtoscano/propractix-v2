package com.propractix.academicinstitution.adapter.out.persistence;

import com.propractix.academicinstitution.application.port.out.CatalogInstitutionCandidate;
import com.propractix.academicinstitution.application.port.out.CatalogPublicationRepository;
import com.propractix.academicinstitution.domain.event.InstitutionCatalogPublished;
import com.propractix.academicinstitution.domain.model.AcademicInstitution;
import com.propractix.academicinstitution.domain.model.CatalogImport;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.ExternalInstitutionReference;
import com.propractix.academicinstitution.domain.service.CatalogPublicationPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class PostgresCatalogPublicationAdapter implements CatalogPublicationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresCatalogPublicationAdapter.class);
    private static final long CATALOG_PUBLICATION_LOCK_KEY = 0x505250585F434154L;

    private final CatalogImportPersistenceRepository catalogImports;
    private final AcademicInstitutionPersistenceRepository institutions;
    private final CatalogImportPersistenceMapper catalogImportMapper;
    private final AcademicInstitutionPersistenceMapper institutionMapper;
    private final CatalogPublicationPolicy publicationPolicy;
    private final TransactionTemplate transactionTemplate;

    public PostgresCatalogPublicationAdapter(DataSource dataSource, CatalogPublicationPolicy publicationPolicy) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        this.catalogImports = new CatalogImportPersistenceRepository(jdbcTemplate);
        this.institutions = new AcademicInstitutionPersistenceRepository(jdbcTemplate);
        this.catalogImportMapper = new CatalogImportPersistenceMapper();
        this.institutionMapper = new AcademicInstitutionPersistenceMapper();
        this.publicationPolicy = Objects.requireNonNull(publicationPolicy, "publicationPolicy must not be null");
        this.transactionTemplate = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public PublicationOutcome publishAtomically(PublicationRequest request, PublicationResources resources) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(resources, "resources must not be null");
        try {
            PublicationOutcome outcome = transactionTemplate.execute(status -> publishInTransaction(request, resources));
            return Objects.requireNonNull(outcome, "publication transaction returned no outcome");
        } catch (InvalidPublicationException exception) {
            LOGGER.warn("Catalog publication rejected with category=invalid_publication");
            return new Rejected(Failure.INVALID_PUBLICATION);
        } catch (ReconciliationConflictException | DuplicateKeyException exception) {
            LOGGER.warn("Catalog publication rejected with category=reconciliation_conflict");
            return new Rejected(Failure.RECONCILIATION_CONFLICT);
        } catch (DataAccessException
                | TransactionException
                | PersistenceMappingException
                | IllegalArgumentException exception) {
            LOGGER.error("Catalog publication rejected with category=storage_failure");
            return new Rejected(Failure.STORAGE_FAILURE);
        }
    }

    private PublicationOutcome publishInTransaction(PublicationRequest request, PublicationResources resources) {
        if (!catalogImports.tryAcquirePublicationLock(CATALOG_PUBLICATION_LOCK_KEY)) {
            return new PublicationInProgress();
        }

        Optional<CatalogImportJpaEntity> activeEntity = catalogImports.findPublished();
        Optional<CatalogImport> activeImport = activeEntity.map(catalogImportMapper::toDomain);
        CatalogPublicationPolicy.PublicationDecision decision =
                publicationPolicy.evaluate(activeImport, request.metadata().artifactHash());
        if (decision == CatalogPublicationPolicy.PublicationDecision.ACTIVE_ARTIFACT_UNCHANGED) {
            CatalogImport current = activeImport.orElseThrow();
            return new ActiveArtifactUnchanged(snapshotOf(current));
        }

        validateChangedPublication(request);
        Map<ExternalInstitutionReference, CatalogInstitutionCandidate> candidates = candidatesByReference(request);
        Map<ExternalInstitutionReference, AcademicInstitutionJpaEntity> existing =
                institutions.findByReferences(new ArrayList<>(candidates.keySet()));

        Instant publishedAt = resources.now();
        CatalogImportId importId = resources.nextCatalogImportId();
        CatalogImport newImport = CatalogImport.published(
                importId,
                request.schemaVersion(),
                request.metadata(),
                request.totalRowCount(),
                request.candidates().size(),
                request.omittedRowCount(),
                publishedAt,
                activeImport.map(CatalogImport::id));

        activeImport.ifPresent(current -> {
            current.markSuperseded(publishedAt);
            CatalogImportJpaEntity entity = activeEntity.orElseThrow();
            catalogImportMapper.updateEntity(entity, current);
            catalogImports.markSuperseded(entity);
        });
        catalogImports.insert(catalogImportMapper.toEntity(newImport));

        List<AcademicInstitutionJpaEntity> updates = new ArrayList<>();
        List<AcademicInstitutionJpaEntity> inserts = new ArrayList<>();
        for (Map.Entry<ExternalInstitutionReference, CatalogInstitutionCandidate> entry : candidates.entrySet()) {
            CatalogInstitutionCandidate candidate = entry.getValue();
            AcademicInstitutionJpaEntity persisted = existing.get(entry.getKey());
            if (persisted == null) {
                AcademicInstitution institution = AcademicInstitution.published(
                        resources.nextAcademicInstitutionId(),
                        candidate.externalReference(),
                        candidate.officialName(),
                        candidate.countryCode(),
                        candidate.sourceStatus(),
                        importId);
                inserts.add(institutionMapper.toNewEntity(institution, publishedAt));
            } else {
                AcademicInstitution institution = institutionMapper.toDomain(persisted);
                institution.publishIn(
                        importId,
                        candidate.officialName(),
                        candidate.countryCode(),
                        candidate.sourceStatus());
                institutionMapper.updateEntity(persisted, institution, publishedAt);
                updates.add(persisted);
            }
        }
        institutions.updateAll(updates);
        institutions.insertAll(inserts);
        activeImport.ifPresent(current ->
                institutions.withdrawPublishedBySnapshot(current.id().value(), publishedAt));

        InstitutionCatalogPublished event = publicationPolicy
                .eventFor(decision, Optional.of(newImport))
                .orElseThrow();
        return new Published(snapshotOf(newImport), event);
    }

    private static Map<ExternalInstitutionReference, CatalogInstitutionCandidate> candidatesByReference(
            PublicationRequest request) {
        Map<ExternalInstitutionReference, CatalogInstitutionCandidate> candidates = new HashMap<>();
        for (CatalogInstitutionCandidate candidate : request.candidates()) {
            CatalogInstitutionCandidate previous = candidates.putIfAbsent(candidate.externalReference(), candidate);
            if (previous != null) {
                if (previous.equals(candidate)) {
                    throw new InvalidPublicationException();
                }
                throw new ReconciliationConflictException();
            }
        }
        return candidates;
    }

    private static void validateChangedPublication(PublicationRequest request) {
        if (request.schemaVersion().length() > 100) {
            throw new InvalidPublicationException();
        }
        for (CatalogInstitutionCandidate candidate : request.candidates()) {
            ExternalInstitutionReference reference = candidate.externalReference();
            if (reference.sourceSystem().length() > 100
                    || reference.sourceRecordId().length() > 200
                    || candidate.sourceStatus().filter(value -> value.length() > 100).isPresent()) {
                throw new InvalidPublicationException();
            }
        }
    }

    private static PublishedSnapshot snapshotOf(CatalogImport catalogImport) {
        return new PublishedSnapshot(
                catalogImport.id(),
                catalogImport.metadata().artifactHash(),
                catalogImport.publishedAt().orElseThrow());
    }
}
