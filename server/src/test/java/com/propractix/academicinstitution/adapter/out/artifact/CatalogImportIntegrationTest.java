package com.propractix.academicinstitution.adapter.out.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.propractix.academicinstitution.adapter.out.persistence.PostgresCatalogPublicationAdapter;
import com.propractix.academicinstitution.application.command.PublishInstitutionCatalogCommand;
import com.propractix.academicinstitution.application.port.in.CatalogPublicationResult;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.service.PublishInstitutionCatalogService;
import com.propractix.academicinstitution.configuration.CatalogImportPublicationConfiguration;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.service.CatalogPublicationPolicy;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class CatalogImportIntegrationTest {

    private static final String FIXTURE_HASH = "612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a";
    private static final Instant RETRIEVED_AT = Instant.parse("2026-09-21T00:00:00Z");
    private static final AtomicReference<Instant> CLOCK =
            new AtomicReference<>(Instant.parse("2026-10-18T10:00:00Z"));

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    private static JdbcTemplate jdbcTemplate;
    private static PublishInstitutionCatalogService service;
    private static AtomicInteger institutionIds;
    private static AtomicInteger importIds;

    @BeforeAll
    static void configureImporter() {
        DataSource dataSource =
                new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        institutionIds = new AtomicInteger();
        importIds = new AtomicInteger();
        service = new PublishInstitutionCatalogService(
                new StrictCsvCatalogArtifactParser(Set.of("SYNTHETIC"), Set.of("ES")),
                new PostgresCatalogPublicationAdapter(dataSource, new CatalogPublicationPolicy()),
                CLOCK::get,
                () -> {
                    institutionIds.incrementAndGet();
                    return new AcademicInstitutionId(UUID.randomUUID());
                },
                () -> {
                    importIds.incrementAndGet();
                    return new CatalogImportId(UUID.randomUUID());
                });
    }

    @BeforeEach
    void clearCatalog() {
        jdbcTemplate.execute(
                "TRUNCATE academicinstitution_institution, academicinstitution_catalog_import CASCADE");
        institutionIds.set(0);
        importIds.set(0);
        CLOCK.set(Instant.parse("2026-10-18T10:00:00Z"));
    }

    @Test
    void publishesTheCompleteSyntheticFixtureAndRepeatsTheActiveArtifactIdempotently() throws IOException {
        byte[] fixture = readFixture();

        CatalogPublicationResult.Published first = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(fixture, new CatalogArtifactHash(FIXTURE_HASH))));
        CatalogPublicationResult.ActiveArtifactUnchanged repeated = assertInstanceOf(
                CatalogPublicationResult.ActiveArtifactUnchanged.class,
                service.publish(command(fixture, new CatalogArtifactHash(FIXTURE_HASH))));

        assertEquals(first.catalogImportId(), repeated.catalogImportId());
        assertEquals(1, count("academicinstitution_catalog_import"));
        assertEquals(3, count("academicinstitution_institution"));
        assertEquals(3, institutionIds.get());
        assertEquals(1, importIds.get());
        assertEquals("SYNTHETIC", jdbcTemplate.queryForObject(
                "SELECT min(source_system) FROM academicinstitution_institution", String.class));
        assertEquals(FIXTURE_HASH, jdbcTemplate.queryForObject(
                "SELECT artifact_hash FROM academicinstitution_catalog_import WHERE status = 'PUBLISHED'",
                String.class));
        assertEquals("https://example.invalid/propractix/academic-institutions-es-synthetic-v1.csv",
                jdbcTemplate.queryForObject(
                        "SELECT source_url FROM academicinstitution_catalog_import WHERE status = 'PUBLISHED'",
                        String.class));
    }

    @Test
    void administrativeContextWiresTheE2UseCaseAndE3AdapterWithoutAWebServer() throws IOException {
        new ApplicationContextRunner()
                .withUserConfiguration(CatalogImportPublicationConfiguration.class)
                .withPropertyValues(
                        "propractix.academic-institution.catalog-import.enabled=true",
                        "propractix.academic-institution.catalog-import.allowed-source-systems=SYNTHETIC",
                        "propractix.academic-institution.catalog-import.allowed-countries=ES",
                        "propractix.academic-institution.catalog-import.artifact-path=fixture.csv",
                        "propractix.academic-institution.catalog-import.schema-version=academic-institution-catalog/v1",
                        "propractix.academic-institution.catalog-import.source-url=https://example.invalid/catalog.csv",
                        "propractix.academic-institution.catalog-import.retrieved-at=2026-09-21T00:00:00Z",
                        "propractix.academic-institution.catalog-import.source-attribution=Synthetic test attribution",
                        "propractix.academic-institution.catalog-import.artifact-hash=" + FIXTURE_HASH,
                        "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRES.getUsername(),
                        "spring.datasource.password=" + POSTGRES.getPassword())
                .run(context -> {
            assertNull(context.getStartupFailure());
            PublishInstitutionCatalog publisher = context.getBean(PublishInstitutionCatalog.class);

            CatalogPublicationResult.Published published = assertInstanceOf(
                    CatalogPublicationResult.Published.class,
                    publisher.publish(command(readFixture(), new CatalogArtifactHash(FIXTURE_HASH))));

            assertEquals(FIXTURE_HASH, published.artifactHash().value());
            assertEquals(1, count("academicinstitution_catalog_import"));
            assertEquals(3, count("academicinstitution_institution"));
        });
    }

    @Test
    void invalidArtifactLeavesTheLastValidSnapshotUntouched() throws IOException {
        byte[] fixture = readFixture();
        CatalogPublicationResult.Published first = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(fixture, new CatalogArtifactHash(FIXTURE_HASH))));
        byte[] invalid = new String(fixture, StandardCharsets.UTF_8)
                .replace("\n", "\r\n")
                .getBytes(StandardCharsets.UTF_8);

        CatalogPublicationResult.Rejected rejected = assertInstanceOf(
                CatalogPublicationResult.Rejected.class, service.publish(command(invalid, hashOf(invalid))));

        assertEquals(CatalogPublicationResult.Failure.ARTIFACT_INVALID, rejected.failure());
        assertEquals(first.catalogImportId().value(), jdbcTemplate.queryForObject(
                "SELECT catalog_import_id FROM academicinstitution_catalog_import WHERE status = 'PUBLISHED'",
                UUID.class));
        assertEquals(1, count("academicinstitution_catalog_import"));
        assertEquals(3, count("academicinstitution_institution"));
    }

    @Test
    void headerOnlyDoesNotChangeAnyPublishedSnapshotDataOrConsumeUuids() throws IOException {
        service.publish(command(readFixture(), new CatalogArtifactHash(FIXTURE_HASH)));
        var importsBefore = jdbcTemplate.queryForList(
                "SELECT * FROM academicinstitution_catalog_import ORDER BY catalog_import_id");
        var institutionsBefore = jdbcTemplate.queryForList(
                "SELECT * FROM academicinstitution_institution ORDER BY institution_id");
        int institutionIdsBefore = institutionIds.get();
        int importIdsBefore = importIds.get();
        byte[] headerOnly = (StrictCsvCatalogArtifactParser.EXACT_HEADER + "\n")
                .getBytes(StandardCharsets.UTF_8);

        CatalogPublicationResult.Rejected rejected = assertInstanceOf(
                CatalogPublicationResult.Rejected.class,
                service.publish(command(headerOnly, hashOf(headerOnly))));

        assertEquals(CatalogPublicationResult.Failure.ARTIFACT_INVALID, rejected.failure());
        assertEquals(importsBefore, jdbcTemplate.queryForList(
                "SELECT * FROM academicinstitution_catalog_import ORDER BY catalog_import_id"));
        assertEquals(institutionsBefore, jdbcTemplate.queryForList(
                "SELECT * FROM academicinstitution_institution ORDER BY institution_id"));
        assertEquals(institutionIdsBefore, institutionIds.get());
        assertEquals(importIdsBefore, importIds.get());
    }

    @Test
    void storageFailureThroughTheE2UseCaseRollsBackAndPreservesThePreviousSnapshot() throws IOException {
        byte[] fixture = readFixture();
        CatalogPublicationResult.Published first = assertInstanceOf(
                CatalogPublicationResult.Published.class,
                service.publish(command(fixture, new CatalogArtifactHash(FIXTURE_HASH))));
        jdbcTemplate.execute("""
                CREATE FUNCTION fail_e4_catalog_import() RETURNS trigger AS $$
                BEGIN
                    IF NEW.official_name = 'FAIL STORAGE' THEN
                        RAISE EXCEPTION 'injected catalog persistence failure';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("""
                CREATE TRIGGER fail_e4_catalog_import_trigger
                BEFORE INSERT OR UPDATE ON academicinstitution_institution
                FOR EACH ROW EXECUTE FUNCTION fail_e4_catalog_import()
                """);
        try {
            byte[] failing = (StrictCsvCatalogArtifactParser.EXACT_HEADER + "\n"
                            + "SYNTHETIC,TEST-0001,FAIL STORAGE,ES,\n")
                    .getBytes(StandardCharsets.UTF_8);
            CLOCK.set(Instant.parse("2026-10-18T11:00:00Z"));

            CatalogPublicationResult.Rejected rejected = assertInstanceOf(
                    CatalogPublicationResult.Rejected.class, service.publish(command(failing, hashOf(failing))));

            assertEquals(CatalogPublicationResult.Failure.PUBLICATION_REJECTED, rejected.failure());
            assertEquals(first.catalogImportId().value(), jdbcTemplate.queryForObject(
                    "SELECT catalog_import_id FROM academicinstitution_catalog_import WHERE status = 'PUBLISHED'",
                    UUID.class));
            assertEquals(1, count("academicinstitution_catalog_import"));
            assertEquals(3, count("academicinstitution_institution"));
        } finally {
            jdbcTemplate.execute(
                    "DROP TRIGGER fail_e4_catalog_import_trigger ON academicinstitution_institution");
            jdbcTemplate.execute("DROP FUNCTION fail_e4_catalog_import()");
        }
    }

    private static PublishInstitutionCatalogCommand command(byte[] bytes, CatalogArtifactHash artifactHash) {
        return new PublishInstitutionCatalogCommand(
                bytes,
                "academic-institution-catalog/v1",
                URI.create("https://example.invalid/propractix/academic-institutions-es-synthetic-v1.csv"),
                RETRIEVED_AT,
                "ProPractix synthetic academic institution fixture for automated tests only",
                artifactHash,
                Optional.empty());
    }

    private static byte[] readFixture() throws IOException {
        return Files.readAllBytes(
                Path.of("..", "docs", "catalog", "fixtures", "academic-institutions-es-synthetic-v1.csv"));
    }

    private static CatalogArtifactHash hashOf(byte[] bytes) {
        try {
            return new CatalogArtifactHash(
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private static int count(String table) {
        if (!table.equals("academicinstitution_catalog_import")
                && !table.equals("academicinstitution_institution")) {
            throw new IllegalArgumentException("unexpected table");
        }
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
