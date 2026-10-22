package com.propractix.academicinstitution.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineAdapter;
import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineAdapter.Operation;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.port.out.AcademicInstitutionIdGenerator;
import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import com.propractix.academicinstitution.application.port.out.CatalogImportIdGenerator;
import com.propractix.academicinstitution.application.port.out.CatalogPublicationRepository;
import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Isolated("Temporarily selects Boot's logging system through a JVM system property")
class CatalogImportApplicationIntegrationTest {

    private static final String HEADER = "sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus\n";
    private static final String PREFIX = "propractix.academic-institution.catalog-import.";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @TempDir
    Path temporaryDirectory;

    private String previousLoggingSystem;
    private Level previousRootLevel;
    private List<Appender<ILoggingEvent>> previousRootAppenders;

    @BeforeEach
    void leaveLogbackUnderTheTestRunnersControl() {
        previousLoggingSystem = System.getProperty(LoggingSystem.SYSTEM_PROPERTY);
        previousRootLevel = rootLogger().getLevel();
        previousRootAppenders = rootAppenders();
        // These in-process Boot contexts test import wiring. They must not apply
        // the CLI's root=OFF configuration to the test runner's shared Logback.
        System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
    }

    @AfterEach
    void restoreLoggingSystemSelectionAndVerifyLogbackWasNotReconfigured() {
        // Restore even when a test or the Logback assertions fail.
        if (previousLoggingSystem == null) {
            System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
        } else {
            System.setProperty(LoggingSystem.SYSTEM_PROPERTY, previousLoggingSystem);
        }
        assertThat(rootLogger().getLevel()).isEqualTo(previousRootLevel);
        assertThat(rootAppenders()).containsExactlyElementsOf(previousRootAppenders);
    }

    @Test
    void bothModesUseBootExternalConfigAndImportsWhileDryRunHasNoPublicationInfrastructure() throws Exception {
        byte[] artifact = (HEADER + "SYNTHETIC_EXTERNAL,0001,Université Synthétique,FR,\n")
                .getBytes(StandardCharsets.UTF_8);
        Path artifactPath = temporaryDirectory.resolve("external.csv");
        Files.write(artifactPath, artifact);
        String artifactHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(artifact));
        Path manifest = temporaryDirectory.resolve("manifest.properties");
        Files.writeString(manifest, """
                %senabled=true
                %sallowed-source-systems=SYNTHETIC_EXTERNAL
                %sallowed-countries=FR
                %sartifact-path=%s
                %sartifact-hash=%s
                %sschema-version=academic-institution-catalog/v1
                %ssource-url=https://example.invalid/external.csv
                %sretrieved-at=2026-10-19T00:00:00Z
                %ssource-attribution=External synthetic test attribution
                %ssource-download-hash=%s
                """.formatted(PREFIX, PREFIX, PREFIX, PREFIX, artifactPath, PREFIX, artifactHash,
                        PREFIX, PREFIX, PREFIX, PREFIX, PREFIX, "b".repeat(64)));
        Path externalConfiguration = temporaryDirectory.resolve("application-test.properties");
        Files.writeString(externalConfiguration, "spring.config.import=" + manifest.toUri() + "\n");
        String[] arguments = {
            "--spring.profiles.active=test",
            "--spring.config.additional-location=" + temporaryDirectory.toUri(),
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword(),
            "--spring.docker.compose.enabled=false",
            "--logging.config=classpath:logback-catalog-import.xml"
        };
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        CatalogImportProperties dryRunProperties;
        try (ConfigurableApplicationContext dryRun =
                CatalogImportCommandLineApplication.startContext(Operation.DRY_RUN, arguments)) {
            dryRunProperties = dryRun.getBean(CatalogImportProperties.class);
            assertThat(dryRunProperties.artifactPath()).isEqualTo(artifactPath);
            assertThat(dryRunProperties.artifactHash()).isEqualTo(artifactHash);
            assertThat(dryRunProperties.allowedSourceSystems()).containsExactly("SYNTHETIC_EXTERNAL");
            assertThat(dryRunProperties.allowedCountries()).containsExactly("FR");
            assertThat(dryRun.getBeansOfType(DataSource.class)).isEmpty();
            assertThat(dryRun.getBeansOfType(Flyway.class)).isEmpty();
            assertThat(dryRun.getBeansOfType(EntityManagerFactory.class)).isEmpty();
            assertThat(dryRun.getBeansOfType(PlatformTransactionManager.class)).isEmpty();
            assertThat(dryRun.getBeansOfType(CatalogPublicationRepository.class)).isEmpty();
            assertThat(dryRun.getBeansOfType(AcademicInstitutionIdGenerator.class)).isEmpty();
            assertThat(dryRun.getBeansOfType(CatalogImportIdGenerator.class)).isEmpty();
            assertThat(dryRun.getBeansOfType(PublishInstitutionCatalog.class)).isEmpty();
            assertParserUsesExternalAllowLists(dryRun, artifact);
            assertExecution(dryRun, Operation.DRY_RUN, 0, "code=catalog_artifact_valid", artifactHash);
        }
        assertThat(jdbc.queryForObject("SELECT to_regclass('flyway_schema_history')::text", String.class)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT to_regclass('academicinstitution_catalog_import')::text", String.class)).isNull();

        try (ConfigurableApplicationContext publication =
                CatalogImportCommandLineApplication.startContext(Operation.PUBLISH, arguments)) {
            assertThat(publication.getBean(CatalogImportProperties.class)).isEqualTo(dryRunProperties);
            assertParserUsesExternalAllowLists(publication, artifact);
            assertExecution(publication, Operation.PUBLISH, 0, "code=catalog_published", artifactHash);
        }
        var stored = jdbc.queryForMap("SELECT * FROM academicinstitution_catalog_import WHERE status = 'PUBLISHED'");
        assertThat(stored.get("artifact_hash")).isEqualTo(artifactHash);
        assertThat(stored.get("source_url")).isEqualTo(dryRunProperties.sourceUrl().toString());
        assertThat(stored.get("retrieved_at")).isEqualTo(java.sql.Timestamp.from(dryRunProperties.retrievedAt()));
        assertThat(stored.get("source_attribution")).isEqualTo(dryRunProperties.sourceAttribution());
        assertThat(stored.get("source_download_hash")).isEqualTo(dryRunProperties.sourceDownloadHash());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM academicinstitution_institution", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void headerOnlyThroughBootDryRunReturnsArtifactRejectionInsteadOfConfigurationFailure() throws Exception {
        Path artifact = temporaryDirectory.resolve("header-only.csv");
        Files.writeString(artifact, HEADER);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact)));
        var bytes = new ByteArrayOutputStream();
        try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            int exit = CatalogImportCommandLineApplication.execute(Operation.DRY_RUN, output,
                    "--spring.profiles.active=test",
                    "--" + PREFIX + "artifact-path=" + artifact,
                    "--" + PREFIX + "artifact-hash=" + hash,
                    "--logging.config=classpath:logback-catalog-import.xml");
            assertThat(exit).isEqualTo(4);
        }
        assertThat(bytes.toString(StandardCharsets.UTF_8))
                .contains("code=catalog_artifact_rejected reason=invalid_schema")
                .doesNotContain("configuration_invalid");
    }

    private static void assertParserUsesExternalAllowLists(ConfigurableApplicationContext context, byte[] artifact) {
        var parser = context.getBean(CatalogArtifactParser.class);
        assertThat(parser.parse(artifact)).isInstanceOf(CatalogArtifactParser.ParsedArtifact.class);
        for (String disallowed : new String[] {
                HEADER + "SYNTHETIC,0001,Université Synthétique,FR,\n",
                HEADER + "SYNTHETIC_EXTERNAL,0001,Université Synthétique,ES,\n"
        }) {
            assertThat(parser.parse(disallowed.getBytes(StandardCharsets.UTF_8)))
                    .isEqualTo(new CatalogArtifactParser.RejectedArtifact(CatalogArtifactParser.Failure.INVALID_CONTENT));
        }
    }

    private static Logger rootLogger() {
        return (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    private static List<Appender<ILoggingEvent>> rootAppenders() {
        List<Appender<ILoggingEvent>> appenders = new ArrayList<>();
        rootLogger().iteratorForAppenders().forEachRemaining(appenders::add);
        return List.copyOf(appenders);
    }

    private static void assertExecution(ConfigurableApplicationContext context, Operation operation,
            int expectedExit, String expectedCode, String expectedHash) {
        var publisher = operation == Operation.PUBLISH
                ? java.util.Optional.of(context.getBean(PublishInstitutionCatalog.class))
                : java.util.Optional.<PublishInstitutionCatalog>empty();
        var adapter = new CatalogImportCommandLineAdapter(
                context.getBean(CatalogImportProperties.class).toDefinition(),
                context.getBean(CatalogArtifactParser.class), publisher);
        var bytes = new ByteArrayOutputStream();
        try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            assertThat(adapter.execute(operation, output)).isEqualTo(expectedExit);
        }
        assertThat(bytes.toString(StandardCharsets.UTF_8)).contains(expectedCode, "artifact_hash=" + expectedHash);
    }
}
