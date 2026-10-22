package com.propractix.academicinstitution.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class CatalogImportPropertiesTest {

    @Test
    void validatesCompleteEnabledConfiguration() {
        CatalogImportProperties properties = new CatalogImportProperties(
                true,
                Set.of("SYNTHETIC"),
                Set.of("ES"),
                Path.of("fixture.csv"),
                "academic-institution-catalog/v1",
                URI.create("https://example.invalid/catalog.csv"),
                Instant.parse("2026-09-21T00:00:00Z"),
                "Synthetic attribution",
                "a".repeat(64),
                null);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties)).isEmpty();
        }
        assertThat(properties.toDefinition().sourceDownloadHash()).isEmpty();
    }

    @Test
    void rejectsIncompleteEnabledConfigurationAtTheBindingBoundary() {
        CatalogImportProperties properties = new CatalogImportProperties(
                true, Set.of("SYNTHETIC"), Set.of("ES"), null, null, null, null, null, null, null);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("enabledDefinitionComplete");
        }
    }

    @Test
    void productionAndStagingExplicitlyRejectSyntheticAndDisableManualImport() throws Exception {
        assertNonSyntheticDisabledProfile("application-production.yaml");
        assertNonSyntheticDisabledProfile("application-staging.yaml");
    }

    @Test
    void localAndTestExplicitlyAllowOnlyTheSyntheticFixture() throws Exception {
        assertSyntheticEnabledProfile("application-local.yaml");
        assertSyntheticEnabledProfile("application-test.yaml");
    }

    private static void assertNonSyntheticDisabledProfile(String resource) throws Exception {
        var propertySource = new YamlPropertySourceLoader()
                .load(resource, new ClassPathResource(resource)).getFirst();
        String prefix = "propractix.academic-institution.catalog-import.";

        assertThat(propertySource.getProperty(prefix + "enabled")).isEqualTo(false);
        assertThat(propertySource.getProperty(prefix + "allowed-source-systems[0]")).isEqualTo("RUCT");
        assertThat(propertySource.getProperty(prefix + "allowed-source-systems[1]")).isNull();
        assertThat(propertySource.getProperty(prefix + "artifact-path")).isNull();
    }

    private static void assertSyntheticEnabledProfile(String resource) throws Exception {
        var propertySource = new YamlPropertySourceLoader()
                .load(resource, new ClassPathResource(resource)).getFirst();
        String prefix = "propractix.academic-institution.catalog-import.";

        assertThat(propertySource.getProperty(prefix + "enabled")).isEqualTo(true);
        assertThat(propertySource.getProperty(prefix + "allowed-source-systems[0]")).isEqualTo("SYNTHETIC");
        assertThat(propertySource.getProperty(prefix + "allowed-source-systems[1]")).isNull();
        assertThat(propertySource.getProperty(prefix + "artifact-hash"))
                .isEqualTo("612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a");
    }
}
