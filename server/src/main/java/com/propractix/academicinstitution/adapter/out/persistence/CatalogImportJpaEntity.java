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
@Table(name = "academicinstitution_catalog_import")
final class CatalogImportJpaEntity {

    @Id
    @Column(name = "catalog_import_id", nullable = false)
    private UUID catalogImportId;

    @Column(name = "schema_version", nullable = false, length = 100)
    private String schemaVersion;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "retrieved_at", nullable = false)
    private Instant retrievedAt;

    @Column(name = "source_attribution", nullable = false)
    private String sourceAttribution;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "artifact_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String artifactHash;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_download_hash", length = 64, columnDefinition = "char(64)")
    private String sourceDownloadHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_row_count", nullable = false)
    private int totalRowCount;

    @Column(name = "published_row_count", nullable = false)
    private int publishedRowCount;

    @Column(name = "omitted_row_count", nullable = false)
    private int omittedRowCount;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "previous_published_import_id")
    private UUID previousPublishedImportId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CatalogImportJpaEntity() {}

    CatalogImportJpaEntity(
            UUID catalogImportId,
            String schemaVersion,
            String sourceUrl,
            Instant retrievedAt,
            String sourceAttribution,
            String artifactHash,
            String sourceDownloadHash,
            String status,
            int totalRowCount,
            int publishedRowCount,
            int omittedRowCount,
            String failureCode,
            Instant createdAt,
            Instant publishedAt,
            Instant supersededAt,
            UUID previousPublishedImportId,
            long version) {
        this.catalogImportId = catalogImportId;
        this.schemaVersion = schemaVersion;
        this.sourceUrl = sourceUrl;
        this.retrievedAt = retrievedAt;
        this.sourceAttribution = sourceAttribution;
        this.artifactHash = artifactHash;
        this.sourceDownloadHash = sourceDownloadHash;
        this.status = status;
        this.totalRowCount = totalRowCount;
        this.publishedRowCount = publishedRowCount;
        this.omittedRowCount = omittedRowCount;
        this.failureCode = failureCode;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
        this.supersededAt = supersededAt;
        this.previousPublishedImportId = previousPublishedImportId;
        this.version = version;
    }

    UUID catalogImportId() {
        return catalogImportId;
    }

    String schemaVersion() {
        return schemaVersion;
    }

    String sourceUrl() {
        return sourceUrl;
    }

    Instant retrievedAt() {
        return retrievedAt;
    }

    String sourceAttribution() {
        return sourceAttribution;
    }

    String artifactHash() {
        return artifactHash;
    }

    String sourceDownloadHash() {
        return sourceDownloadHash;
    }

    String status() {
        return status;
    }

    int totalRowCount() {
        return totalRowCount;
    }

    int publishedRowCount() {
        return publishedRowCount;
    }

    int omittedRowCount() {
        return omittedRowCount;
    }

    String failureCode() {
        return failureCode;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant publishedAt() {
        return publishedAt;
    }

    Instant supersededAt() {
        return supersededAt;
    }

    UUID previousPublishedImportId() {
        return previousPublishedImportId;
    }

    long version() {
        return version;
    }

    void markSuperseded(Instant at) {
        status = "SUPERSEDED";
        supersededAt = at;
    }
}
