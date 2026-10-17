package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

class OfficialInstitutionNameTest {

    @Test
    void rejectsAnEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName(""));
    }

    @Test
    void acceptsExactlyThreeHundredCodePointsAndRejectsThreeHundredAndOne() {
        OfficialInstitutionName name = new OfficialInstitutionName("𐐀".repeat(300));

        assertEquals(300, name.value().codePointCount(0, name.value().length()));
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName("𐐀".repeat(301)));
    }

    @Test
    void normalizesUnicodeToNfcWhilePreservingSpellingAndAccents() {
        OfficialInstitutionName name = new OfficialInstitutionName("Universidad de Ma\u0301laga");

        assertEquals("Universidad de Málaga", name.value());
        assertEquals(Normalizer.Form.NFC, normalizedFormOf(name.value()));
    }

    @Test
    void rejectsLeadingAndTrailingWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName(" Universidad de Málaga"));
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName("Universidad de Málaga "));
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName("\u00a0Universidad de Málaga"));
    }

    @Test
    void rejectsControlsAndEveryLineBreakCategory() {
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName("Universidad\tde Málaga"));
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName("Universidad\nde Málaga"));
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName("Universidad\rde Málaga"));
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName("Universidad\u2028de Málaga"));
        assertThrows(IllegalArgumentException.class, () -> new OfficialInstitutionName("Universidad\u2029de Málaga"));
    }

    @Test
    void equalityAndHashCodeUseTheNormalizedValue() {
        OfficialInstitutionName first = new OfficialInstitutionName("Universidad de Ma\u0301laga");
        OfficialInstitutionName equal = new OfficialInstitutionName("Universidad de Málaga");
        OfficialInstitutionName different = new OfficialInstitutionName("Universidad de Sevilla");

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, different);
    }

    private static Normalizer.Form normalizedFormOf(String value) {
        if (Normalizer.isNormalized(value, Normalizer.Form.NFC)) {
            return Normalizer.Form.NFC;
        }
        throw new AssertionError("value is not normalized to NFC");
    }
}
