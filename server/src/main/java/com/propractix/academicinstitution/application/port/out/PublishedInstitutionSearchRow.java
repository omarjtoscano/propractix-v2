package com.propractix.academicinstitution.application.port.out;

import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import java.util.Objects;

public record PublishedInstitutionSearchRow(
        AcademicInstitutionId id,
        OfficialInstitutionName officialName,
        CountryCode countryCode,
        String searchNameNormalized) {

    public PublishedInstitutionSearchRow {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(officialName, "officialName must not be null");
        Objects.requireNonNull(countryCode, "countryCode must not be null");
        Objects.requireNonNull(searchNameNormalized, "searchNameNormalized must not be null");
        if (searchNameNormalized.isBlank()) {
            throw new IllegalArgumentException("searchNameNormalized must not be blank");
        }
    }
}
