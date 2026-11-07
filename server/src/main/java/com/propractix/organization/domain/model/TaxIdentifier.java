package com.propractix.organization.domain.model;

import java.util.Objects;

/** Country/type-specific normalization and validation belong to the future adapter. */
public record TaxIdentifier(String type, String normalizedValue) {
    public TaxIdentifier {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(normalizedValue, "normalizedValue must not be null");
        if (!type.matches("[A-Z][A-Z0-9_]*") || normalizedValue.isBlank()
                || !normalizedValue.equals(normalizedValue.strip())
                || normalizedValue.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("tax identifier must have a stable type and a normalized value");
        }
    }

    @Override
    public String toString() {
        return "TaxIdentifier[redacted]";
    }
}
