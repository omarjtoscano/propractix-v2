package com.propractix.academicinstitution.application.query;

import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import java.util.Objects;

public record AcademicInstitutionSearchItem(
        AcademicInstitutionId id, OfficialInstitutionName officialName, CountryCode countryCode) {

    public AcademicInstitutionSearchItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(officialName, "officialName must not be null");
        Objects.requireNonNull(countryCode, "countryCode must not be null");
    }
}
