package com.propractix.organization.domain.service;

import com.propractix.organization.domain.model.CompanyProfile;
import com.propractix.organization.domain.model.CountryCode;
import com.propractix.organization.domain.model.LocaleCode;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Typed catalog snapshot supplied externally; resolves presentation only, never jurisdiction. */
public record CompanyRegistrationPolicy(
        Set<CountryCode> enabledCountries,
        Set<LocaleCode> supportedLocales,
        Map<CountryCode, LocaleCode> countryLocales,
        LocaleCode fallbackLocale) {
    public CompanyRegistrationPolicy {
        enabledCountries = Set.copyOf(enabledCountries);
        supportedLocales = Set.copyOf(supportedLocales);
        countryLocales = Map.copyOf(countryLocales);
        Objects.requireNonNull(fallbackLocale, "fallbackLocale must not be null");
        if (enabledCountries.isEmpty() || !supportedLocales.contains(fallbackLocale)
                || !enabledCountries.containsAll(countryLocales.keySet())
                || !supportedLocales.containsAll(countryLocales.values())) {
            throw new IllegalArgumentException("inconsistent registration catalogs");
        }
    }

    public LocaleCode initialLocale(CompanyProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        if (!enabledCountries.contains(profile.registeredCountry())) {
            throw new IllegalArgumentException("registered country is not enabled");
        }
        return profile.preferredLocale().filter(supportedLocales::contains)
                .orElseGet(() -> countryLocales.getOrDefault(profile.registeredCountry(), fallbackLocale));
    }
}
