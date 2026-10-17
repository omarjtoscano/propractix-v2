package com.propractix.academicinstitution.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CatalogImportId(UUID value) {

    public CatalogImportId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.version() != 4 || value.variant() != 2) {
            throw new IllegalArgumentException("value must be an RFC 4122 UUID v4");
        }
    }

    public static CatalogImportId from(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return new CatalogImportId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
