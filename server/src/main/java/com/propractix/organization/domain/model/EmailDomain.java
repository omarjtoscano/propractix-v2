package com.propractix.organization.domain.model;

import java.util.Objects;

/**
 * Structural validation of canonical ASCII supplied by the technical boundary.
 * This does not decode Punycode or establish Unicode/IDNA conformance (ADR-001).
 */
public record EmailDomain(String value) {
    public EmailDomain {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isEmpty() || value.length() > 253 || value.matches("[0-9]+(?:\\.[0-9]+){3}")) {
            throw new IllegalArgumentException("invalid canonical email domain length or IP literal");
        }
        for (String label : value.split("\\.", -1)) {
            if (label.length() > 63 || !label.matches("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")
                    || (label.length() >= 4 && label.substring(2, 4).equals("--")
                        && !label.startsWith("xn--"))) {
                throw new IllegalArgumentException("email domain must contain canonical lowercase ASCII labels");
            }
        }
    }

    @Override
    public String toString() {
        return "EmailDomain[redacted]";
    }
}
