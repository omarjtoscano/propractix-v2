package com.propractix.organization.domain.model;

import java.util.Objects;

/** No trimming, case folding, alias rewriting or Unicode normalization occurs here. */
public record EmailAddress(String value) {
    public EmailAddress {
        Objects.requireNonNull(value, "value must not be null");
        int separator = value.indexOf('@');
        if (separator < 1 || separator != value.lastIndexOf('@') || separator > 64 || value.length() > 254) {
            throw new IllegalArgumentException("invalid canonical email address length or separator");
        }
        String local = value.substring(0, separator);
        if (!local.matches("[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*")) {
            throw new IllegalArgumentException("local part must be an ASCII dot-atom");
        }
        new EmailDomain(value.substring(separator + 1));
    }

    public EmailDomain domain() {
        return new EmailDomain(value.substring(value.indexOf('@') + 1));
    }

    @Override
    public String toString() {
        return "EmailAddress[redacted]";
    }
}
