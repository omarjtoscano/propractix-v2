package com.propractix.organization.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CompanyValueObjectsTest {
    @Test
    void validatesUuidVersionAndVariantWithoutGeneratingIdentifiers() {
        var id = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");
        assertEquals(id, new TenantId(id).value());
        assertEquals(id, new OnboardingId(id).value());
        for (String invalid : new String[]{"00000000-0000-0000-0000-000000000000",
                "123e4567-e89b-12d3-a456-426614174000", "123e4567-e89b-42d3-7456-426614174000"}) {
            assertThrows(IllegalArgumentException.class, () -> new TenantId(UUID.fromString(invalid)));
            assertThrows(IllegalArgumentException.class, () -> new OnboardingId(UUID.fromString(invalid)));
        }
        assertThrows(NullPointerException.class, () -> new TenantId(null));
        assertThrows(NullPointerException.class, () -> new OnboardingId(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "es", "ESP", "ZZ", " ES"})
    void rejectsInvalidOrNonCanonicalCountryCodes(String value) {
        assertThrows(IllegalArgumentException.class, () -> new CountryCode(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "und", "es_ES", "ES", "en-us", "en-", " es"})
    void rejectsInvalidOrNonCanonicalLocales(String value) {
        assertThrows(IllegalArgumentException.class, () -> new LocaleCode(value));
    }

    @Test
    void keepsNamedZonesAndCanonicalLanguageTagsIndependentOfCountry() {
        assertEquals("en-US", new LocaleCode("en-US").value());
        assertEquals("Europe/Berlin", new CompanyTimeZone("Europe/Berlin").value());
        assertEquals("UTC", new CompanyTimeZone("UTC").value());
        assertThrows(IllegalArgumentException.class, () -> new CompanyTimeZone("+01:00"));
        assertThrows(IllegalArgumentException.class, () -> new CompanyTimeZone("No/Such_Zone"));
        assertThrows(NullPointerException.class, () -> new CompanyTimeZone(null));
        assertThrows(NullPointerException.class, () -> new LocaleCode(null));
        assertThrows(NullPointerException.class, () -> new CountryCode(null));
    }

    @Test
    void fiscalIdentityIsOpaqueAndHasNoEmbeddedSpanishRules() {
        var tax = new TaxIdentifier("SYNTHETIC_FOREIGN", "SYNTHETIC-X-0001");
        assertEquals("SYNTHETIC_FOREIGN", tax.type());
        assertEquals("SYNTHETIC-X-0001", tax.normalizedValue());
        assertEquals("TaxIdentifier[redacted]", tax.toString());
        assertThrows(IllegalArgumentException.class, () -> new TaxIdentifier("", "SYNTHETIC"));
        assertThrows(IllegalArgumentException.class, () -> new TaxIdentifier("lowercase", "SYNTHETIC"));
        assertThrows(IllegalArgumentException.class, () -> new TaxIdentifier("SYNTHETIC", " "));
        assertThrows(IllegalArgumentException.class, () -> new TaxIdentifier("SYNTHETIC", " X "));
        assertThrows(IllegalArgumentException.class, () -> new TaxIdentifier("SYNTHETIC", "X\n"));
        assertThrows(NullPointerException.class, () -> new TaxIdentifier(null, "SYNTHETIC"));
        assertThrows(NullPointerException.class, () -> new TaxIdentifier("SYNTHETIC", null));
    }

    @Test
    void namesRejectControlsAndBlankValuesAndDoNotLeakIntoDiagnostics() {
        assertEquals("Empresa Sintética", new CompanyName("Empresa Sintética").value());
        assertEquals("CompanyName[redacted]", new CompanyName("Empresa Sintética").toString());
        assertThrows(IllegalArgumentException.class, () -> new CompanyName(" "));
        assertThrows(IllegalArgumentException.class, () -> new CompanyName("Empresa\n"));
        assertThrows(NullPointerException.class, () -> new CompanyName(null));
    }
}
