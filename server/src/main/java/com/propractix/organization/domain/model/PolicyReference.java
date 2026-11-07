package com.propractix.organization.domain.model;

import java.util.Objects;

public record PolicyReference(String id, String version) {
    public PolicyReference {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(version, "version must not be null");
        if (id.isBlank() || version.isBlank()) {
            throw new IllegalArgumentException("policy id and version must be non-blank");
        }
    }
}
