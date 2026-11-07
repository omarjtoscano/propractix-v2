package com.propractix.organization.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record CountryCode(String value) {
    private static final Set<String> ISO_CODES = Set.of(Locale.getISOCountries());

    public CountryCode {
        Objects.requireNonNull(value, "value must not be null");
        if (!ISO_CODES.contains(value)) {
            throw new IllegalArgumentException("value must be a canonical ISO alpha-2 country code");
        }
    }
}
