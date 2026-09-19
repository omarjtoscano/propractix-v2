package com.propractix.configuration.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProductCatalogPropertiesTest {

    @Test
    void acceptsCompleteConfiguration() {
        var properties = new ProductCatalogProperties(
                Set.of("ES"), Set.of("es", "en"), Map.of("ES", "es"), "es");

        assertThat(properties.isInternallyConsistent()).isTrue();
    }

    @Test
    void rejectsAMappingToAnUnsupportedLocale() {
        var properties = new ProductCatalogProperties(
                Set.of("ES"), Set.of("es", "en"), Map.of("ES", "fr"), "es");

        assertThat(properties.isInternallyConsistent()).isFalse();
    }

    @Test
    void rejectsFallbackOutsideSupportedLocales() {
        var properties = new ProductCatalogProperties(
                Set.of("ES"), Set.of("es", "en"), Map.of("ES", "es"), "fr");

        assertThat(properties.isInternallyConsistent()).isFalse();
    }

    @Test
    void beanValidationRejectsInconsistentConfigurationAtBindingBoundary() {
        var properties = new ProductCatalogProperties(
                Set.of("ES"), Set.of("es", "en"), Map.of("ES", "fr"), "fr");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(properties))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("internallyConsistent");
        }
    }
}
