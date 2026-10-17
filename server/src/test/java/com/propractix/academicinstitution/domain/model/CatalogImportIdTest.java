package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CatalogImportIdTest {

    @Test
    void acceptsOnlyUuidVersionFour() {
        CatalogImportId id = CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000");

        assertEquals("223e4567-e89b-42d3-a456-426614174000", id.toString());
        assertThrows(
                IllegalArgumentException.class,
                () -> CatalogImportId.from("223e4567-e89b-12d3-a456-426614174000"));
        assertThrows(IllegalArgumentException.class, () -> CatalogImportId.from("not-a-uuid"));
    }

    @Test
    void equalityAndHashCodeUseTheUuidValue() {
        CatalogImportId first = CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000");
        CatalogImportId equal = CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000");
        CatalogImportId different = CatalogImportId.from("223e4567-e89b-42d3-a456-426614174001");

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, different);
    }
}
