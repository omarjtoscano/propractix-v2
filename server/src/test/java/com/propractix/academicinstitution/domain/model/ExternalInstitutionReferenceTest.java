package com.propractix.academicinstitution.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ExternalInstitutionReferenceTest {

    @Test
    void rejectsEmptyParts() {
        assertThrows(IllegalArgumentException.class, () -> new ExternalInstitutionReference("", "0007"));
        assertThrows(IllegalArgumentException.class, () -> new ExternalInstitutionReference("RUCT", "  "));
    }

    @Test
    void normalizesOnlyExteriorWhitespaceAndPreservesLeadingZeroes() {
        ExternalInstitutionReference reference = new ExternalInstitutionReference("\u00a0 RUCT ", " 000007\u00a0");

        assertEquals("RUCT", reference.sourceSystem());
        assertEquals("000007", reference.sourceRecordId());
    }

    @Test
    void equalityAndHashCodeUseBothOpaqueParts() {
        ExternalInstitutionReference first = new ExternalInstitutionReference("RUCT", "000007");
        ExternalInstitutionReference equal = new ExternalInstitutionReference("\u00a0RUCT ", " 000007\u00a0");
        ExternalInstitutionReference different = new ExternalInstitutionReference("RUCT", "7");

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, different);
    }
}
