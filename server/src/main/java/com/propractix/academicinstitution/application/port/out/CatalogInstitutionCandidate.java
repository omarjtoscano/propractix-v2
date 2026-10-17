package com.propractix.academicinstitution.application.port.out;

import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.ExternalInstitutionReference;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import java.util.Objects;
import java.util.Optional;

public record CatalogInstitutionCandidate(
        ExternalInstitutionReference externalReference,
        OfficialInstitutionName officialName,
        CountryCode countryCode,
        Optional<String> sourceStatus) {

    public CatalogInstitutionCandidate {
        Objects.requireNonNull(externalReference, "externalReference must not be null");
        Objects.requireNonNull(officialName, "officialName must not be null");
        Objects.requireNonNull(countryCode, "countryCode must not be null");
        Objects.requireNonNull(sourceStatus, "sourceStatus must not be null");
        sourceStatus = sourceStatus.map(value -> {
            Objects.requireNonNull(value, "sourceStatus value must not be null");
            if (value.isBlank()) {
                throw new IllegalArgumentException("sourceStatus must be absent or non-blank");
            }
            return value;
        });
    }
}
