package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AcademicInstitutionTest {

    @Test
    void keepsInternalIdentityIndependentFromTheRuctReference() {
        ExternalInstitutionReference ructReference = new ExternalInstitutionReference("RUCT", "000007");
        AcademicInstitution first = institution(
                AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000"),
                ructReference,
                Optional.empty());
        AcademicInstitution second = institution(
                AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174001"),
                ructReference,
                Optional.empty());

        assertNotEquals(first.id(), second.id());
        assertEquals(first.externalReference(), second.externalReference());
        assertEquals("000007", first.externalReference().sourceRecordId());
    }

    @Test
    void keepsSourceStatusAbsentWithoutInference() {
        AcademicInstitution institution = institution(
                AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000"),
                new ExternalInstitutionReference("RUCT", "000007"),
                Optional.empty());

        assertEquals(Optional.empty(), institution.sourceStatus());
    }

    @Test
    void preservesAnExplicitSourceStatusAndRejectsAnEmptyPresentValue() {
        AcademicInstitution institution = institution(
                AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000"),
                new ExternalInstitutionReference("RUCT", "000007"),
                Optional.of("  ACTIVE "));

        assertEquals(Optional.of("ACTIVE"), institution.sourceStatus());
        assertThrows(
                IllegalArgumentException.class,
                () -> institution(
                        AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000"),
                        new ExternalInstitutionReference("RUCT", "000007"),
                        Optional.of(" ")));
    }

    private static AcademicInstitution institution(
            AcademicInstitutionId id,
            ExternalInstitutionReference reference,
            Optional<String> sourceStatus) {
        return new AcademicInstitution(
                id,
                reference,
                new OfficialInstitutionName("Universidad de Málaga"),
                new CountryCode("ES"),
                sourceStatus,
                CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000"));
    }
}
