package com.propractix.organization.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OnboardingId(UUID value) {
    public OnboardingId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.version() != 4 || value.variant() != 2) {
            throw new IllegalArgumentException("value must be an RFC 4122 UUID v4");
        }
    }
}
