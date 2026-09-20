package com.propractix.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "propractix.runtime", ignoreUnknownFields = false)
public record RuntimeMetadataProperties(
        @NotBlank String environment,
        @NotBlank String releaseSha) {
}
