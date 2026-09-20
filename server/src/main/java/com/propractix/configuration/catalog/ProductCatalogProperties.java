package com.propractix.configuration.catalog;

import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@Validated
@ConfigurationProperties(prefix = "propractix.catalog", ignoreUnknownFields = false)
public record ProductCatalogProperties(
        @NotEmpty Set<@Pattern(regexp = "[A-Z]{2}") String> enabledCountries,
        @NotEmpty Set<@Pattern(regexp = "[a-z]{2}(?:-[A-Z]{2})?") String> supportedLocales,
        @NotEmpty Map<@Pattern(regexp = "[A-Z]{2}") String,
                @Pattern(regexp = "[a-z]{2}(?:-[A-Z]{2})?") String> countryDefaultLocales,
        @NotBlank @Pattern(regexp = "[a-z]{2}(?:-[A-Z]{2})?") String fallbackLocale) {

    public ProductCatalogProperties {
        enabledCountries = enabledCountries == null ? Set.of() : Set.copyOf(enabledCountries);
        supportedLocales = supportedLocales == null ? Set.of() : Set.copyOf(supportedLocales);
        countryDefaultLocales = countryDefaultLocales == null ? Map.of() : Map.copyOf(countryDefaultLocales);
    }

    @AssertTrue(message = "catalog mappings and fallback must reference enabled values")
    public boolean isInternallyConsistent() {
        return supportedLocales.contains(fallbackLocale)
                && countryDefaultLocales.keySet().equals(enabledCountries)
                && supportedLocales.containsAll(countryDefaultLocales.values());
    }
}
