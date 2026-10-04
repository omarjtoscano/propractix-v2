package com.propractix.academicinstitution.domain.model;

import java.util.Objects;
import java.util.Optional;

public final class AcademicInstitution {

    private final AcademicInstitutionId id;
    private final ExternalInstitutionReference externalReference;
    private final OfficialInstitutionName officialName;
    private final CountryCode countryCode;
    private final Optional<String> sourceStatus;
    private final CatalogImportId publishedCatalogImportId;

    public AcademicInstitution(
            AcademicInstitutionId id,
            ExternalInstitutionReference externalReference,
            OfficialInstitutionName officialName,
            CountryCode countryCode,
            Optional<String> sourceStatus,
            CatalogImportId publishedCatalogImportId) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.externalReference = Objects.requireNonNull(externalReference, "externalReference must not be null");
        this.officialName = Objects.requireNonNull(officialName, "officialName must not be null");
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode must not be null");
        this.sourceStatus = normalizeSourceStatus(sourceStatus);
        this.publishedCatalogImportId = Objects.requireNonNull(
                publishedCatalogImportId, "publishedCatalogImportId must not be null");
    }

    public AcademicInstitutionId id() {
        return id;
    }

    public ExternalInstitutionReference externalReference() {
        return externalReference;
    }

    public OfficialInstitutionName officialName() {
        return officialName;
    }

    public CountryCode countryCode() {
        return countryCode;
    }

    public Optional<String> sourceStatus() {
        return sourceStatus;
    }

    public CatalogImportId publishedCatalogImportId() {
        return publishedCatalogImportId;
    }

    private static Optional<String> normalizeSourceStatus(Optional<String> sourceStatus) {
        Objects.requireNonNull(sourceStatus, "sourceStatus must not be null");
        return sourceStatus.map(value -> {
            String normalized = Objects.requireNonNull(value, "sourceStatus value must not be null").strip();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("sourceStatus must be absent or non-blank");
            }
            return normalized;
        });
    }
}
