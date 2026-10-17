package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcademicInstitutionIdTest {

    @Test
    void acceptsAnRfc4122UuidV4() {
        AcademicInstitutionId id = AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000");

        assertEquals(UUID.fromString("123e4567-e89b-42d3-a456-426614174000"), id.value());
    }

    @Test
    void rejectsMalformedAndNonVersionFourUuids() {
        assertThrows(IllegalArgumentException.class, () -> AcademicInstitutionId.from("not-a-uuid"));
        assertThrows(
                IllegalArgumentException.class,
                () -> AcademicInstitutionId.from("123e4567-e89b-12d3-a456-426614174000"));
        assertThrows(
                IllegalArgumentException.class,
                () -> AcademicInstitutionId.from("123e4567-e89b-42d3-7456-426614174000"));
    }

    @Test
    void equalityAndHashCodeUseTheUuidValue() {
        AcademicInstitutionId first = AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000");
        AcademicInstitutionId equal = AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000");
        AcademicInstitutionId different = AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174001");

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, different);
    }
}
