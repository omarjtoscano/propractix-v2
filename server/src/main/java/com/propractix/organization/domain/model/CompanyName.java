package com.propractix.organization.domain.model;

import java.util.Objects;

public record CompanyName(String value) {
    public CompanyName {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank() || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("company name must be non-blank and contain no controls");
        }
    }

    @Override
    public String toString() {
        return "CompanyName[redacted]";
    }
}
