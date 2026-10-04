package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class AcademicInstitutionTest {

    private static final AcademicInstitutionId INSTITUTION_ID =
            AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000");
    private static final CatalogImportId FIRST_IMPORT_ID =
            CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000");
    private static final CatalogImportId SECOND_IMPORT_ID =
            CatalogImportId.from("223e4567-e89b-42d3-a456-426614174001");

    @Test
    void aPublishedInstitutionKnowsItsSnapshot() {
        AcademicInstitution institution = institution(
                INSTITUTION_ID,
                new ExternalInstitutionReference("RUCT", "000007"),
                Optional.empty());

        assertEquals(Optional.of(FIRST_IMPORT_ID), institution.publishedCatalogImportId());
    }

    @Test
    void withdrawingAnInstitutionPreservesItsIdentityAndExternalReference() {
        ExternalInstitutionReference reference = new ExternalInstitutionReference("RUCT", "000007");
        AcademicInstitution institution = institution(INSTITUTION_ID, reference, Optional.empty());

        institution.withdraw();

        assertEquals(INSTITUTION_ID, institution.id());
        assertEquals(reference, institution.externalReference());
        assertEquals(Optional.empty(), institution.publishedCatalogImportId());
    }

    @Test
    void aWithdrawnInstitutionCanReappearInANewSnapshotWithTheSameIdentity() {
        ExternalInstitutionReference reference = new ExternalInstitutionReference("RUCT", "000007");
        AcademicInstitution institution = AcademicInstitution.withdrawn(
                INSTITUTION_ID,
                reference,
                new OfficialInstitutionName("Universidad de Málaga"),
                new CountryCode("ES"),
                Optional.empty());

        institution.publishIn(
                SECOND_IMPORT_ID,
                new OfficialInstitutionName("Universidad de Málaga Actualizada"),
                new CountryCode("ES"),
                Optional.of("ACTIVE"));

        assertEquals(INSTITUTION_ID, institution.id());
        assertEquals(reference, institution.externalReference());
        assertEquals(Optional.of(SECOND_IMPORT_ID), institution.publishedCatalogImportId());
        assertEquals("Universidad de Málaga Actualizada", institution.officialName().value());
        assertEquals(Optional.of("ACTIVE"), institution.sourceStatus());
    }

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
        return AcademicInstitution.published(
                id,
                reference,
                new OfficialInstitutionName("Universidad de Málaga"),
                new CountryCode("ES"),
                sourceStatus,
                FIRST_IMPORT_ID);
    }
}
