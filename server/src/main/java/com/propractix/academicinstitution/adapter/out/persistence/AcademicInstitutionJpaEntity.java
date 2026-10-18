package com.propractix.academicinstitution.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "academicinstitution_institution")
final class AcademicInstitutionJpaEntity {

    @Id
    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "source_record_id", nullable = false, length = 200)
    private String sourceRecordId;

    @Column(name = "official_name", nullable = false, length = 300)
    private String officialName;

    @Column(name = "search_name_normalized", nullable = false, length = 300)
    private String searchNameNormalized;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false, length = 2, columnDefinition = "char(2)")
    private String countryCode;

    @Column(name = "source_status", length = 100)
    private String sourceStatus;

    @Column(name = "published_catalog_import_id")
    private UUID publishedCatalogImportId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AcademicInstitutionJpaEntity() {}

    AcademicInstitutionJpaEntity(
            UUID institutionId,
            String sourceSystem,
            String sourceRecordId,
            String officialName,
            String searchNameNormalized,
            String countryCode,
            String sourceStatus,
            UUID publishedCatalogImportId,
            Instant createdAt,
            Instant updatedAt,
            long version) {
        this.institutionId = institutionId;
        this.sourceSystem = sourceSystem;
        this.sourceRecordId = sourceRecordId;
        this.officialName = officialName;
        this.searchNameNormalized = searchNameNormalized;
        this.countryCode = countryCode;
        this.sourceStatus = sourceStatus;
        this.publishedCatalogImportId = publishedCatalogImportId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    UUID institutionId() {
        return institutionId;
    }

    String sourceSystem() {
        return sourceSystem;
    }

    String sourceRecordId() {
        return sourceRecordId;
    }

    String officialName() {
        return officialName;
    }

    String searchNameNormalized() {
        return searchNameNormalized;
    }

    String countryCode() {
        return countryCode;
    }

    String sourceStatus() {
        return sourceStatus;
    }

    UUID publishedCatalogImportId() {
        return publishedCatalogImportId;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    long version() {
        return version;
    }

    void replacePublication(
            String officialName,
            String searchNameNormalized,
            String countryCode,
            String sourceStatus,
            UUID publishedCatalogImportId,
            Instant updatedAt) {
        this.officialName = officialName;
        this.searchNameNormalized = searchNameNormalized;
        this.countryCode = countryCode;
        this.sourceStatus = sourceStatus;
        this.publishedCatalogImportId = publishedCatalogImportId;
        this.updatedAt = updatedAt;
    }
}
