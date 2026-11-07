package com.propractix.organization.domain.model;

import java.time.ZoneId;
import java.util.Objects;

public record CompanyTimeZone(String value) {
    public CompanyTimeZone {
        Objects.requireNonNull(value, "value must not be null");
        if (!ZoneId.getAvailableZoneIds().contains(value)) {
            throw new IllegalArgumentException("value must be a named IANA time zone");
        }
    }
}
