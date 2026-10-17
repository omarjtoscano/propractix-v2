package com.propractix.academicinstitution.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record CountryCode(String value) {

    private static final Set<String> ISO_ALPHA_2_CODES = Set.of(Locale.getISOCountries());

    public CountryCode {
        Objects.requireNonNull(value, "value must not be null");
        value = value.strip().toUpperCase(Locale.ROOT);
        if (!ISO_ALPHA_2_CODES.contains(value)) {
            throw new IllegalArgumentException("value must be an ISO 3166-1 alpha-2 country code");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
