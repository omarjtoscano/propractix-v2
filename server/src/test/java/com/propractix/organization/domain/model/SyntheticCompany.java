package com.propractix.organization.domain.model;

import com.propractix.organization.domain.service.CompanyEmailAdmissionPolicy;
import com.propractix.organization.domain.service.CompanyRegistrationPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** LOCAL_TEST / TEST_ONLY: in-memory synthetic data, never a published policy or IDNA engine double. */
public final class SyntheticCompany {
    public static final Instant AT = Instant.parse("2026-11-02T10:00:00Z");
    public static final Duration VALIDITY = Duration.parse("P7D");
    public static final TenantId TENANT = new TenantId(UUID.fromString("123e4567-e89b-42d3-a456-426614174000"));
    public static final OnboardingId ONBOARDING = new OnboardingId(UUID.fromString("123e4567-e89b-42d3-a456-426614174001"));
    public static final CountryCode ES = new CountryCode("ES");
    public static final CountryCode DE = new CountryCode("DE");
    public static final LocaleCode SPANISH = new LocaleCode("es");
    public static final LocaleCode ENGLISH = new LocaleCode("en");

    private SyntheticCompany() { }

    public static CompanyProfile profile() {
        return profile(ES, Optional.empty());
    }

    public static CompanyProfile profile(CountryCode country, Optional<LocaleCode> locale) {
        return new CompanyProfile(new CompanyName("Empresa Sintética E1"), Optional.empty(), country,
                new TaxIdentifier("SYNTHETIC", "SYNTHETIC-E1-0001"), locale, new CompanyTimeZone("Europe/Madrid"));
    }

    public static CompanyRegistrationPolicy registration() {
        return new CompanyRegistrationPolicy(Set.of(ES, DE), Set.of(SPANISH, ENGLISH),
                Map.of(ES, SPANISH), SPANISH);
    }

    public static CompanyEmailAdmissionPolicy admission() {
        return new CompanyEmailAdmissionPolicy(new PolicyReference("synthetic-admission", "local-test-1"),
                Set.of(new EmailDomain("bloqueado.test")));
    }

    public static Company company() {
        return Company.selfDeclared(TENANT, profile(), registration());
    }

    public static CompanyOnboarding submitted() {
        return CompanyOnboarding.submit(ONBOARDING, profile(), new EmailAddress("Persona+qa@empresa.test"),
                admission(), registration(), AT, VALIDITY);
    }
}
