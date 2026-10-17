package com.propractix.academicinstitution.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchPublishedInstitutionsQueryTest {

    @Test
    void createsNormalizedAccentInsensitiveTerm() {
        NormalizedInstitutionSearchTerm term =
                NormalizedInstitutionSearchTerm.from("  UNIVERSIDAD   de Ma\u0301laga  ");

        assertEquals("universidad de malaga", term.value());
    }

    @Test
    void rejectsTermsThatAreNotAlreadyNormalizedWhenConstructedDirectly() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new NormalizedInstitutionSearchTerm("Universidad de Málaga"));
    }

    @Test
    void acceptsMinimumAndMaximumLimitsAndRejectsOutsideValues() {
        new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 1);
        new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 50);

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 51));
    }

    @Test
    void cursorMustBeBoundToTheSameNormalizedQuery() {
        SearchPublishedInstitutionsQuery original = new SearchPublishedInstitutionsQuery(
                Optional.of(NormalizedInstitutionSearchTerm.from("Málaga")), Optional.empty(), 20);
        InstitutionSearchCursor cursor = InstitutionSearchCursor.after(
                CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000"),
                original.queryFingerprint(),
                "universidad de malaga",
                AcademicInstitutionId.from("123e4567-e89b-42d3-a456-426614174000"));

        new SearchPublishedInstitutionsQuery(original.term(), Optional.of(cursor), 20);
        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchPublishedInstitutionsQuery(
                        Optional.of(NormalizedInstitutionSearchTerm.from("Sevilla")),
                        Optional.of(cursor),
                        20));
    }
}
