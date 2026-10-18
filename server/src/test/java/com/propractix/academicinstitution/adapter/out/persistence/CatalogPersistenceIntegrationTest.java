package com.propractix.academicinstitution.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutionsResult;
import com.propractix.academicinstitution.application.port.out.CatalogInstitutionCandidate;
import com.propractix.academicinstitution.application.port.out.CatalogPublicationRepository;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionQueryRepository;
import com.propractix.academicinstitution.application.query.InstitutionSearchCursor;
import com.propractix.academicinstitution.application.query.NormalizedInstitutionSearchTerm;
import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;
import com.propractix.academicinstitution.application.service.SearchPublishedInstitutionsService;
import com.propractix.academicinstitution.domain.model.AcademicInstitution;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CatalogImportMetadata;
import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.ExternalInstitutionReference;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import com.propractix.academicinstitution.domain.service.CatalogPublicationPolicy;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class CatalogPersistenceIntegrationTest {

    private static final Instant FIRST_PUBLICATION = Instant.parse("2026-10-17T10:00:00Z");
    private static final Instant SECOND_PUBLICATION = Instant.parse("2026-10-17T11:00:00Z");
    private static final URI SOURCE_URL = URI.create("https://example.test/catalog.csv");

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    private static DataSource dataSource;
    private static JdbcTemplate jdbcTemplate;
    private static PostgresCatalogPublicationAdapter publicationAdapter;
    private static PostgresPublishedInstitutionQueryAdapter queryAdapter;

    @BeforeAll
    static void migrateEmptyPostgreSql16Database() {
        DriverManagerDataSource configuredDataSource =
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        dataSource = configuredDataSource;
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        publicationAdapter = new PostgresCatalogPublicationAdapter(dataSource, new CatalogPublicationPolicy());
        queryAdapter = new PostgresPublishedInstitutionQueryAdapter(dataSource);
    }

    @BeforeEach
    void clearCatalog() {
        jdbcTemplate.execute(
                "TRUNCATE academicinstitution_institution, academicinstitution_catalog_import CASCADE");
    }

    @Test
    void migratesTheCatalogFromEmptyPostgreSql16AndEnforcesCoreConstraints() {
        assertEquals(16, jdbcTemplate.queryForObject(
                "SELECT current_setting('server_version_num')::integer / 10000", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                  FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_name IN (
                       'academicinstitution_catalog_import',
                       'academicinstitution_institution'
                   )
                """,
                Integer.class));

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO academicinstitution_catalog_import (
                    catalog_import_id, schema_version, source_url, retrieved_at,
                    source_attribution, artifact_hash, status, total_row_count,
                    published_row_count, omitted_row_count, created_at, published_at, version
                ) VALUES (?, 'v1', 'https://example.test', ?, 'source', ?, 'PUBLISHED', 0, 0, 0, ?, ?, 0)
                """,
                UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                Timestamp.from(FIRST_PUBLICATION),
                "a".repeat(64),
                Timestamp.from(FIRST_PUBLICATION),
                Timestamp.from(FIRST_PUBLICATION)));

        CatalogPublicationRepository.Published published = assertInstanceOf(
                CatalogPublicationRepository.Published.class,
                publicationAdapter.publishAtomically(
                        request("b", candidate("0001", "Universidad Alfa")),
                        resources(FIRST_PUBLICATION)));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO academicinstitution_catalog_import (
                    catalog_import_id, schema_version, source_url, retrieved_at,
                    source_attribution, artifact_hash, status, total_row_count,
                    published_row_count, omitted_row_count, created_at, published_at, version
                ) VALUES (?, 'v1', 'https://example.test', ?, 'source', ?, 'PUBLISHED', 0, 0, 0, ?, ?, 0)
                """,
                UUID.randomUUID(),
                Timestamp.from(FIRST_PUBLICATION),
                "c".repeat(64),
                Timestamp.from(FIRST_PUBLICATION),
                Timestamp.from(FIRST_PUBLICATION)));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                """
                INSERT INTO academicinstitution_institution (
                    institution_id, source_system, source_record_id, official_name,
                    search_name_normalized, country_code, published_catalog_import_id,
                    created_at, updated_at, version
                ) VALUES (?, 'RUCT', '0001', 'Duplicada', 'duplicada', 'ES', ?, ?, ?, 0)
                """,
                UUID.randomUUID(),
                published.snapshot().catalogImportId().value(),
                Timestamp.from(FIRST_PUBLICATION),
                Timestamp.from(FIRST_PUBLICATION)));
    }

    @Test
    void mapsPersistenceEntitiesWithoutLeakingJpaIntoTheDomain() {
        CatalogImportId snapshotId = new CatalogImportId(UUID.randomUUID());
        AcademicInstitution domain = AcademicInstitution.published(
                new AcademicInstitutionId(UUID.randomUUID()),
                new ExternalInstitutionReference("RUCT", "000123"),
                new OfficialInstitutionName("Universidad de Málaga"),
                new CountryCode("ES"),
                Optional.of("ACTIVE"),
                snapshotId);

        AcademicInstitutionPersistenceMapper mapper = new AcademicInstitutionPersistenceMapper();
        AcademicInstitutionJpaEntity entity = mapper.toNewEntity(domain, FIRST_PUBLICATION);
        AcademicInstitution restored = mapper.toDomain(entity);

        assertEquals(domain.id(), restored.id());
        assertEquals(domain.externalReference(), restored.externalReference());
        assertEquals(domain.officialName(), restored.officialName());
        assertEquals("universidad de malaga", entity.searchNameNormalized());
        assertEquals(Optional.of(snapshotId), restored.publishedCatalogImportId());
    }

    @Test
    void publishesAtomicallyReusesKnownUuidsAndWithdrawsMissingInstitutions() {
        TrackingResources firstResources = resources(FIRST_PUBLICATION);
        CatalogPublicationRepository.Published first = assertInstanceOf(
                CatalogPublicationRepository.Published.class,
                publicationAdapter.publishAtomically(
                        request("a", candidate("0001", "Universidad Alfa"), candidate("0002", "Universidad Beta")),
                        firstResources));
        UUID firstId = institutionId("0001");
        UUID withdrawnId = institutionId("0002");

        TrackingResources secondResources = resources(SECOND_PUBLICATION);
        CatalogPublicationRepository.Published second = assertInstanceOf(
                CatalogPublicationRepository.Published.class,
                publicationAdapter.publishAtomically(
                        request(
                                "b",
                                candidate("0001", "Universidad Alfa Renovada"),
                                candidate("0003", "Universidad Gamma")),
                        secondResources));

        assertNotEquals(first.snapshot().catalogImportId(), second.snapshot().catalogImportId());
        assertEquals(firstId, institutionId("0001"));
        assertEquals(withdrawnId, institutionId("0002"));
        assertEquals(1, secondResources.institutionIdsGenerated());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM academicinstitution_catalog_import WHERE status = 'PUBLISHED'",
                Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM academicinstitution_institution
                 WHERE source_record_id = '0002' AND published_catalog_import_id IS NULL
                """,
                Integer.class));

        TrackingResources thirdResources = resources(SECOND_PUBLICATION.plusSeconds(3600));
        assertInstanceOf(
                CatalogPublicationRepository.Published.class,
                publicationAdapter.publishAtomically(
                        request("c", candidate("0002", "Universidad Beta")), thirdResources));
        assertEquals(withdrawnId, institutionId("0002"));
        assertEquals(0, thirdResources.institutionIdsGenerated());
    }

    @Test
    void repeatingTheActiveArtifactDoesNotWriteOrConsumeResources() {
        CatalogPublicationRepository.Published first = assertInstanceOf(
                CatalogPublicationRepository.Published.class,
                publicationAdapter.publishAtomically(
                        request("a", candidate("0001", "Universidad Alfa")),
                        resources(FIRST_PUBLICATION)));
        int importsBefore = count("academicinstitution_catalog_import");
        int institutionsBefore = count("academicinstitution_institution");

        CatalogPublicationRepository.ActiveArtifactUnchanged unchanged = assertInstanceOf(
                CatalogPublicationRepository.ActiveArtifactUnchanged.class,
                publicationAdapter.publishAtomically(
                        request("a", candidate("0001", "Contenido deliberadamente ignorado")),
                        new FailingResources()));

        assertEquals(first.snapshot(), unchanged.snapshot());
        assertEquals(importsBefore, count("academicinstitution_catalog_import"));
        assertEquals(institutionsBefore, count("academicinstitution_institution"));
    }

    @Test
    void rollsBackEveryPublicationMutationWhenStorageFails() {
        CatalogPublicationRepository.Published first = assertInstanceOf(
                CatalogPublicationRepository.Published.class,
                publicationAdapter.publishAtomically(
                        request("a", candidate("0001", "Universidad Alfa")),
                        resources(FIRST_PUBLICATION)));
        jdbcTemplate.execute("""
                CREATE FUNCTION fail_catalog_publication() RETURNS trigger AS $$
                BEGIN
                    IF NEW.official_name = 'FAIL STORAGE' THEN
                        RAISE EXCEPTION 'injected catalog persistence failure';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_catalog_publication_trigger
                BEFORE INSERT OR UPDATE ON academicinstitution_institution
                FOR EACH ROW EXECUTE FUNCTION fail_catalog_publication()
                """);
        try {
            CatalogPublicationRepository.Rejected rejected = assertInstanceOf(
                    CatalogPublicationRepository.Rejected.class,
                    publicationAdapter.publishAtomically(
                            request("b", candidate("0001", "FAIL STORAGE")),
                            resources(SECOND_PUBLICATION)));

            assertEquals(CatalogPublicationRepository.Failure.STORAGE_FAILURE, rejected.failure());
            assertEquals(1, count("academicinstitution_catalog_import"));
            assertEquals(first.snapshot().catalogImportId().value(), jdbcTemplate.queryForObject(
                    """
                    SELECT catalog_import_id FROM academicinstitution_catalog_import
                     WHERE status = 'PUBLISHED'
                    """,
                    UUID.class));
            assertEquals("Universidad Alfa", jdbcTemplate.queryForObject(
                    "SELECT official_name FROM academicinstitution_institution WHERE source_record_id = '0001'",
                    String.class));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER fail_catalog_publication_trigger ON academicinstitution_institution");
            jdbcTemplate.execute("DROP FUNCTION fail_catalog_publication()");
        }
    }

    @Test
    void distinguishesInvalidPublicationAndReconciliationConflictInternally() {
        CatalogInstitutionCandidate original = candidate("0001", "Universidad Alfa");
        CatalogPublicationRepository.Rejected invalid = assertInstanceOf(
                CatalogPublicationRepository.Rejected.class,
                publicationAdapter.publishAtomically(
                        request("a", original, original), resources(FIRST_PUBLICATION)));
        CatalogPublicationRepository.Rejected conflict = assertInstanceOf(
                CatalogPublicationRepository.Rejected.class,
                publicationAdapter.publishAtomically(
                        request("b", original, candidate("0001", "Universidad Distinta")),
                        resources(FIRST_PUBLICATION)));

        assertEquals(CatalogPublicationRepository.Failure.INVALID_PUBLICATION, invalid.failure());
        assertEquals(CatalogPublicationRepository.Failure.RECONCILIATION_CONFLICT, conflict.failure());
        assertFalse(invalid.toString().contains("0001"));
        assertFalse(conflict.toString().contains("0001"));
        assertEquals(0, count("academicinstitution_catalog_import"));
    }

    @Test
    void paginatesWithKeysetAndRejectsCursorFromAnotherSnapshot() {
        CatalogPublicationRepository.Published first = assertInstanceOf(
                CatalogPublicationRepository.Published.class,
                publicationAdapter.publishAtomically(
                        request(
                                "a",
                                candidate("0001", "Universidad Álava"),
                                candidate("0002", "Universidad Beta"),
                                candidate("0003", "Universidad Cádiz"),
                                candidate("0004", "Universidad Delta")),
                        resources(FIRST_PUBLICATION)));
        SearchPublishedInstitutionsService service = new SearchPublishedInstitutionsService(queryAdapter);
        SearchPublishedInstitutionsResult.Found firstPage = assertInstanceOf(
                SearchPublishedInstitutionsResult.Found.class,
                service.search(new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 2)));
        InstitutionSearchCursor cursor = firstPage.page().nextCursor().orElseThrow();
        SearchPublishedInstitutionsResult.Found secondPage = assertInstanceOf(
                SearchPublishedInstitutionsResult.Found.class,
                service.search(new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.of(cursor), 2)));

        assertEquals(2, firstPage.page().items().size());
        assertEquals(2, secondPage.page().items().size());
        assertTrue(secondPage.page().nextCursor().isEmpty());
        assertTrue(firstPage.page().items().stream().noneMatch(secondPage.page().items()::contains));

        SearchPublishedInstitutionsResult.Found filtered = assertInstanceOf(
                SearchPublishedInstitutionsResult.Found.class,
                service.search(new SearchPublishedInstitutionsQuery(
                        Optional.of(NormalizedInstitutionSearchTerm.from("Cádiz")), Optional.empty(), 10)));
        assertEquals(List.of("Universidad Cádiz"), filtered.page().items().stream()
                .map(item -> item.officialName().value())
                .toList());

        assertInstanceOf(
                CatalogPublicationRepository.Published.class,
                publicationAdapter.publishAtomically(
                        request("b", candidate("0005", "Universidad Épsilon")),
                        resources(SECOND_PUBLICATION)));
        assertInstanceOf(
                SearchPublishedInstitutionsResult.SnapshotChanged.class,
                service.search(new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.of(cursor), 2)));
        assertEquals(first.snapshot().catalogImportId(), cursor.snapshotId());
    }

    @Test
    void returnsTypedUnavailableResultWhenThereIsNoPublishedSnapshot() {
        PublishedInstitutionQueryRepository.CatalogUnavailable unavailable = assertInstanceOf(
                PublishedInstitutionQueryRepository.CatalogUnavailable.class,
                queryAdapter.search(new PublishedInstitutionQueryRepository.SearchRequest(
                        Optional.empty(), Optional.empty(), 2)));

        assertEquals(PublishedInstitutionQueryRepository.Failure.NO_PUBLISHED_CATALOG, unavailable.failure());
    }

    @Test
    void aConcurrentPublisherFailsFastWhileTheLockHolderPublishes() throws Exception {
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releasePublisher = new CountDownLatch(1);
        BlockingResources firstResources =
                new BlockingResources(FIRST_PUBLICATION, lockAcquired, releasePublisher);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<CatalogPublicationRepository.PublicationOutcome> first = executor.submit(() ->
                    publicationAdapter.publishAtomically(
                            request("a", candidate("0001", "Universidad Alfa")), firstResources));
            assertTrue(lockAcquired.await(10, TimeUnit.SECONDS));

            long startedAt = System.nanoTime();
            CatalogPublicationRepository.PublicationOutcome concurrent = publicationAdapter.publishAtomically(
                    request("b", candidate("0002", "Universidad Beta")), resources(SECOND_PUBLICATION));
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            assertInstanceOf(CatalogPublicationRepository.PublicationInProgress.class, concurrent);
            assertTrue(elapsedMillis < 2_000, "the PostgreSQL lock must be non-blocking");
            releasePublisher.countDown();
            assertInstanceOf(CatalogPublicationRepository.Published.class, first.get(10, TimeUnit.SECONDS));
            assertEquals(1, count("academicinstitution_catalog_import"));
            assertEquals(1, count("academicinstitution_institution"));
        } finally {
            releasePublisher.countDown();
            executor.shutdownNow();
        }
    }

    private static CatalogPublicationRepository.PublicationRequest request(
            String hashCharacter, CatalogInstitutionCandidate... candidates) {
        CatalogArtifactHash hash = new CatalogArtifactHash(hashCharacter.repeat(64));
        return new CatalogPublicationRepository.PublicationRequest(
                "academic-institution-catalog/v1",
                new CatalogImportMetadata(
                        SOURCE_URL,
                        Instant.parse("2026-10-16T09:00:00Z"),
                        "Exact source attribution",
                        hash,
                        Optional.empty()),
                List.of(candidates),
                candidates.length,
                0);
    }

    private static CatalogInstitutionCandidate candidate(String sourceRecordId, String name) {
        return new CatalogInstitutionCandidate(
                new ExternalInstitutionReference("RUCT", sourceRecordId),
                new OfficialInstitutionName(name),
                new CountryCode("ES"),
                Optional.empty());
    }

    private static TrackingResources resources(Instant now) {
        return new TrackingResources(now);
    }

    private static UUID institutionId(String sourceRecordId) {
        return jdbcTemplate.queryForObject(
                "SELECT institution_id FROM academicinstitution_institution WHERE source_record_id = ?",
                UUID.class,
                sourceRecordId);
    }

    private static int count(String table) {
        assertTrue(table.equals("academicinstitution_catalog_import")
                || table.equals("academicinstitution_institution"));
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private static class TrackingResources implements CatalogPublicationRepository.PublicationResources {

        private final Instant now;
        private final AtomicInteger institutionIdsGenerated = new AtomicInteger();

        TrackingResources(Instant now) {
            this.now = now;
        }

        @Override
        public AcademicInstitutionId nextAcademicInstitutionId() {
            institutionIdsGenerated.incrementAndGet();
            return new AcademicInstitutionId(UUID.randomUUID());
        }

        @Override
        public CatalogImportId nextCatalogImportId() {
            return new CatalogImportId(UUID.randomUUID());
        }

        @Override
        public Instant now() {
            return now;
        }

        int institutionIdsGenerated() {
            return institutionIdsGenerated.get();
        }
    }

    private static final class BlockingResources extends TrackingResources {

        private final CountDownLatch lockAcquired;
        private final CountDownLatch releasePublisher;

        BlockingResources(Instant now, CountDownLatch lockAcquired, CountDownLatch releasePublisher) {
            super(now);
            this.lockAcquired = lockAcquired;
            this.releasePublisher = releasePublisher;
        }

        @Override
        public Instant now() {
            lockAcquired.countDown();
            try {
                if (!releasePublisher.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out while holding the publication lock");
                }
                return super.now();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("publication interrupted", exception);
            }
        }
    }

    private static final class FailingResources implements CatalogPublicationRepository.PublicationResources {

        @Override
        public AcademicInstitutionId nextAcademicInstitutionId() {
            throw new AssertionError("unchanged publication must not request an institution ID");
        }

        @Override
        public CatalogImportId nextCatalogImportId() {
            throw new AssertionError("unchanged publication must not request an import ID");
        }

        @Override
        public Instant now() {
            throw new AssertionError("unchanged publication must not request the clock");
        }
    }
}
