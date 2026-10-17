package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CatalogArtifactHashTest {

    @Test
    void acceptsUppercaseAndLowercaseHexadecimalAndCanonicalizesToLowercase() {
        CatalogArtifactHash lowercase = new CatalogArtifactHash("ab".repeat(32));
        CatalogArtifactHash uppercase = new CatalogArtifactHash("AB".repeat(32));

        assertEquals("ab".repeat(32), uppercase.value());
        assertEquals(lowercase, uppercase);
        assertEquals(lowercase.hashCode(), uppercase.hashCode());
    }

    @Test
    void rejectsWrongLengthAndNonHexadecimalCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new CatalogArtifactHash("a".repeat(63)));
        assertThrows(IllegalArgumentException.class, () -> new CatalogArtifactHash("a".repeat(65)));
        assertThrows(IllegalArgumentException.class, () -> new CatalogArtifactHash("g".repeat(64)));
    }

    @Test
    void differentHashesAreNotEqual() {
        assertNotEquals(new CatalogArtifactHash("a".repeat(64)), new CatalogArtifactHash("b".repeat(64)));
    }
}
