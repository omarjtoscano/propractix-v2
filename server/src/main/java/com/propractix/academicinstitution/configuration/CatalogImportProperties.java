package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineDefinition;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "propractix.academic-institution.catalog-import", ignoreUnknownFields = false)
public record CatalogImportProperties(
        boolean enabled,
        @NotEmpty Set<@Pattern(regexp = "[A-Z][A-Z0-9_-]{0,99}") String> allowedSourceSystems,
        @NotEmpty Set<@Pattern(regexp = "[A-Z]{2}") String> allowedCountries,
        Path artifactPath,
        String schemaVersion,
        URI sourceUrl,
        Instant retrievedAt,
        String sourceAttribution,
        @Pattern(regexp = "[0-9a-f]{64}") String artifactHash,
        @Pattern(regexp = "[0-9a-f]{64}") String sourceDownloadHash) {

    private static final java.util.regex.Pattern SOURCE_SYSTEM_PATTERN =
            java.util.regex.Pattern.compile("[A-Z][A-Z0-9_-]{0,99}");
    private static final java.util.regex.Pattern COUNTRY_PATTERN = java.util.regex.Pattern.compile("[A-Z]{2}");
    private static final java.util.regex.Pattern HASH_PATTERN = java.util.regex.Pattern.compile("[0-9a-f]{64}");

    public CatalogImportProperties {
        allowedSourceSystems = allowedSourceSystems == null ? Set.of() : Set.copyOf(allowedSourceSystems);
        allowedCountries = allowedCountries == null ? Set.of() : Set.copyOf(allowedCountries);
        if (allowedSourceSystems.isEmpty()
                || allowedSourceSystems.stream().anyMatch(value -> !matches(SOURCE_SYSTEM_PATTERN, value))) {
            throw new IllegalArgumentException("allowed source systems are invalid");
        }
        if (allowedCountries.isEmpty()
                || allowedCountries.stream().anyMatch(value -> !matches(COUNTRY_PATTERN, value))) {
            throw new IllegalArgumentException("allowed countries are invalid");
        }
        if (artifactHash != null && !matches(HASH_PATTERN, artifactHash)) {
            throw new IllegalArgumentException("artifact hash is invalid");
        }
        if (sourceDownloadHash != null && !matches(HASH_PATTERN, sourceDownloadHash)) {
            throw new IllegalArgumentException("source download hash is invalid");
        }
    }

    @AssertTrue(message = "enabled catalog import requires complete artifact metadata")
    public boolean isEnabledDefinitionComplete() {
        return !enabled
                || (artifactPath != null
                        && hasText(schemaVersion)
                        && sourceUrl != null
                        && sourceUrl.isAbsolute()
                        && retrievedAt != null
                        && hasText(sourceAttribution)
                        && artifactHash != null);
    }

    public CatalogImportCommandLineDefinition toDefinition() {
        return new CatalogImportCommandLineDefinition(
                enabled,
                artifactPath,
                schemaVersion,
                sourceUrl,
                retrievedAt,
                sourceAttribution,
                artifactHash == null ? null : new CatalogArtifactHash(artifactHash),
                Optional.ofNullable(sourceDownloadHash).map(CatalogArtifactHash::new));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean matches(java.util.regex.Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }
}
