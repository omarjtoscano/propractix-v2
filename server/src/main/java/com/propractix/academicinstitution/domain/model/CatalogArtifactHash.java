package com.propractix.academicinstitution.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record CatalogArtifactHash(String value) {

    private static final Pattern SHA_256_HEXADECIMAL = Pattern.compile("[0-9a-fA-F]{64}");

    public CatalogArtifactHash {
        Objects.requireNonNull(value, "value must not be null");
        if (!SHA_256_HEXADECIMAL.matcher(value).matches()) {
            throw new IllegalArgumentException("value must be a 64-character SHA-256 hexadecimal hash");
        }
        value = value.toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
