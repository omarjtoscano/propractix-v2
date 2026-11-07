package com.propractix.organization.domain.model;

import java.util.Objects;
import java.util.Optional;

/** Candidate legal identity; syntactic fiscal validity does not establish legal existence. */
public record CompanyProfile(
        CompanyName legalName,
        Optional<CompanyName> tradeName,
        CountryCode registeredCountry,
        TaxIdentifier taxIdentifier,
        Optional<LocaleCode> preferredLocale,
        CompanyTimeZone timeZone) {
    public CompanyProfile {
        Objects.requireNonNull(legalName, "legalName must not be null");
        Objects.requireNonNull(tradeName, "tradeName must not be null");
        Objects.requireNonNull(registeredCountry, "registeredCountry must not be null");
        Objects.requireNonNull(taxIdentifier, "taxIdentifier must not be null");
        Objects.requireNonNull(preferredLocale, "preferredLocale must not be null");
        Objects.requireNonNull(timeZone, "timeZone must not be null");
    }
}
