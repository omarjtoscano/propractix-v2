package com.propractix.organization.domain.model;

import static com.propractix.organization.domain.model.SyntheticCompany.*;
import static org.junit.jupiter.api.Assertions.*;

import com.propractix.organization.domain.service.CompanyRegistrationPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CompanyTest {
    @Test
    void hasOneTenantIdentityAndRemainsSelfDeclared() {
        Company company = company();
        assertEquals(TENANT, company.id());
        assertEquals(profile(), company.profile());
        assertEquals(Company.Status.SELF_DECLARED, company.status());
        assertEquals(Optional.empty(), company.profile().preferredLocale());
        assertEquals(SPANISH, company.initialLocale());
    }

    @Test
    void keepsTradeNameTaxTypeAndExplicitTimeZone() {
        var details = new CompanyProfile(new CompanyName("Empresa Sintética E1"), Optional.of(new CompanyName("Marca E1")),
                DE, new TaxIdentifier("SYNTHETIC_FOREIGN", "SYNTHETIC-E1-DE-2"), Optional.of(ENGLISH),
                new CompanyTimeZone("America/Bogota"));
        Company company = Company.selfDeclared(TENANT, details, registration().initialLocale(details));
        assertEquals(details, company.profile());
        assertEquals(ENGLISH, company.initialLocale());
        assertEquals("America/Bogota", company.profile().timeZone().value());
        assertEquals(Company.Status.SELF_DECLARED, company.status());
    }

    @Test
    void supportedPreferencePrecedesCountryDefault() {
        assertEquals(ENGLISH, Company.selfDeclared(TENANT, profile(ES, Optional.of(ENGLISH)),
                registration().initialLocale(profile(ES, Optional.of(ENGLISH)))).initialLocale());
    }

    @Test
    void unsupportedPreferenceUsesCountryMappingAndThenConfiguredFallbackWithoutChangingCountry() {
        var french = Optional.of(new LocaleCode("fr"));
        assertEquals(SPANISH, Company.selfDeclared(TENANT, profile(ES, french),
                registration().initialLocale(profile(ES, french))).initialLocale());
        var foreign = Company.selfDeclared(TENANT, profile(DE, french),
                registration().initialLocale(profile(DE, french)));
        assertEquals(SPANISH, foreign.initialLocale());
        assertEquals(DE, foreign.profile().registeredCountry());
        assertEquals(french, foreign.profile().preferredLocale());
        assertEquals("SYNTHETIC", foreign.profile().taxIdentifier().type());
        // Resolving a UI locale creates no jurisdiction, policy pack or legal approval.
        assertEquals(Company.Status.SELF_DECLARED, foreign.status());
    }

    @Test
    void alternativeCatalogRequiresNoCountrySpecificAggregateChanges() {
        var canada = new CountryCode("CA");
        var french = new LocaleCode("fr");
        var alternative = new CompanyRegistrationPolicy(Set.of(canada), Set.of(french, ENGLISH),
                Map.of(canada, ENGLISH), french);
        var company = Company.selfDeclared(TENANT, profile(canada, Optional.empty()),
                alternative.initialLocale(profile(canada, Optional.empty())));
        assertEquals(canada, company.profile().registeredCountry());
        assertEquals(ENGLISH, company.initialLocale());
        var fallback = new CompanyRegistrationPolicy(Set.of(canada), Set.of(french), Map.of(), french);
        assertEquals(french, Company.selfDeclared(TENANT, profile(canada, Optional.empty()),
                fallback.initialLocale(profile(canada, Optional.empty()))).initialLocale());
        assertThrows(IllegalArgumentException.class, () -> alternative.initialLocale(profile()));
    }

    @Test
    void registrationCatalogCannotBeMutatedAfterConstruction() {
        var countries = new HashSet<>(Set.of(ES));
        var locales = new HashSet<>(Set.of(SPANISH));
        var mapping = new HashMap<>(Map.of(ES, SPANISH));
        var policy = new CompanyRegistrationPolicy(countries, locales, mapping, SPANISH);
        countries.clear();
        locales.clear();
        mapping.clear();
        assertEquals(SPANISH, Company.selfDeclared(TENANT, profile(), policy.initialLocale(profile())).initialLocale());
        assertThrows(UnsupportedOperationException.class, () -> policy.enabledCountries().clear());
        assertThrows(UnsupportedOperationException.class, () -> policy.supportedLocales().clear());
        assertThrows(UnsupportedOperationException.class, () -> policy.countryLocales().clear());
    }

    @Test
    void rejectsInconsistentCatalogsAndMissingRequiredValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyRegistrationPolicy(Set.of(), Set.of(SPANISH), Map.of(), SPANISH));
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyRegistrationPolicy(Set.of(ES), Set.of(SPANISH), Map.of(), ENGLISH));
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyRegistrationPolicy(Set.of(ES), Set.of(SPANISH), Map.of(DE, SPANISH), SPANISH));
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyRegistrationPolicy(Set.of(ES), Set.of(SPANISH), Map.of(ES, ENGLISH), SPANISH));
        assertThrows(NullPointerException.class, () -> Company.selfDeclared(null, profile(), SPANISH));
        assertThrows(NullPointerException.class, () -> Company.selfDeclared(TENANT, null, SPANISH));
        assertThrows(NullPointerException.class, () -> Company.selfDeclared(TENANT, profile(), null));
        assertThrows(NullPointerException.class, () -> new CompanyProfile(null, Optional.empty(), ES,
                profile().taxIdentifier(), Optional.empty(), profile().timeZone()));
        assertThrows(NullPointerException.class, () -> new CompanyProfile(profile().legalName(), null, ES,
                profile().taxIdentifier(), Optional.empty(), profile().timeZone()));
    }
}
