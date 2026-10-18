package com.propractix.academicinstitution.adapter.out.persistence;

import com.propractix.academicinstitution.application.query.NormalizedInstitutionSearchTerm;
import com.propractix.academicinstitution.domain.model.AcademicInstitution;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.ExternalInstitutionReference;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import java.time.Instant;
import java.util.Optional;

final class AcademicInstitutionPersistenceMapper {

    AcademicInstitution toDomain(AcademicInstitutionJpaEntity entity) {
        AcademicInstitutionId id = new AcademicInstitutionId(entity.institutionId());
        ExternalInstitutionReference reference =
                new ExternalInstitutionReference(entity.sourceSystem(), entity.sourceRecordId());
        OfficialInstitutionName officialName = new OfficialInstitutionName(entity.officialName());
        CountryCode countryCode = new CountryCode(entity.countryCode());
        Optional<String> sourceStatus = Optional.ofNullable(entity.sourceStatus());
        if (entity.publishedCatalogImportId() == null) {
            return AcademicInstitution.withdrawn(id, reference, officialName, countryCode, sourceStatus);
        }
        return AcademicInstitution.published(
                id,
                reference,
                officialName,
                countryCode,
                sourceStatus,
                new CatalogImportId(entity.publishedCatalogImportId()));
    }

    AcademicInstitutionJpaEntity toNewEntity(AcademicInstitution institution, Instant now) {
        ExternalInstitutionReference reference = institution.externalReference();
        return new AcademicInstitutionJpaEntity(
                institution.id().value(),
                reference.sourceSystem(),
                reference.sourceRecordId(),
                institution.officialName().value(),
                searchName(institution.officialName()),
                institution.countryCode().value(),
                institution.sourceStatus().orElse(null),
                institution.publishedCatalogImportId().map(CatalogImportId::value).orElse(null),
                now,
                now,
                0);
    }

    void updateEntity(AcademicInstitutionJpaEntity entity, AcademicInstitution institution, Instant now) {
        entity.replacePublication(
                institution.officialName().value(),
                searchName(institution.officialName()),
                institution.countryCode().value(),
                institution.sourceStatus().orElse(null),
                institution.publishedCatalogImportId().map(CatalogImportId::value).orElse(null),
                now);
    }

    private static String searchName(OfficialInstitutionName officialName) {
        return NormalizedInstitutionSearchTerm.from(officialName.value()).value();
    }
}
