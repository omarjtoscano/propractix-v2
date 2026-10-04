package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CountryCodeTest {

    @Test
    void acceptsAnyIso3166AlphaTwoCountryIndependentlyOfEnabledCountries() {
        assertEquals("ES", new CountryCode("es").value());
        assertEquals("FR", new CountryCode("FR").value());
    }

    @Test
    void rejectsUnknownOrMalformedCountryCodes() {
        assertThrows(IllegalArgumentException.class, () -> new CountryCode("ZZ"));
        assertThrows(IllegalArgumentException.class, () -> new CountryCode("ESP"));
        assertThrows(IllegalArgumentException.class, () -> new CountryCode(""));
    }

    @Test
    void equalityAndHashCodeUseTheCanonicalUppercaseCode() {
        CountryCode first = new CountryCode("es");
        CountryCode equal = new CountryCode(" ES ");
        CountryCode different = new CountryCode("FR");

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, different);
    }
}
