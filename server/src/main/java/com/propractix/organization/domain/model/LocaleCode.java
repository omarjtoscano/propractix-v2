package com.propractix.organization.domain.model;

import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Objects;

public record LocaleCode(String value) {
    public LocaleCode {
        Objects.requireNonNull(value, "value must not be null");
        Locale locale;
        try {
            locale = new Locale.Builder().setLanguageTag(value).build();
        } catch (IllformedLocaleException invalid) {
            throw new IllegalArgumentException("value must be a canonical language tag");
        }
        if (locale.getLanguage().isEmpty() || !locale.toLanguageTag().equals(value)) {
            throw new IllegalArgumentException("value must be a canonical language tag");
        }
    }
}
