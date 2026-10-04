package com.propractix.academicinstitution.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class CatalogImport {

    public enum Status {
        PUBLISHED,
        SUPERSEDED,
        REJECTED
    }

    private final CatalogImportId id;
    private final String schemaVersion;
    private final CatalogImportMetadata metadata;
    private final int totalRowCount;
    private final int publishedRowCount;
    private final int omittedRowCount;
    private final Instant createdAt;
    private final Optional<Instant> publishedAt;
    private final Optional<CatalogImportId> previousPublishedImportId;
    private final Optional<String> failureCode;
    private Status status;
    private Optional<Instant> supersededAt;

    private CatalogImport(
            CatalogImportId id,
            String schemaVersion,
            CatalogImportMetadata metadata,
            Status status,
            int totalRowCount,
            int publishedRowCount,
            int omittedRowCount,
            Instant createdAt,
            Optional<Instant> publishedAt,
            Optional<Instant> supersededAt,
            Optional<CatalogImportId> previousPublishedImportId,
            Optional<String> failureCode) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.schemaVersion = requireNonBlank(schemaVersion, "schemaVersion");
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.totalRowCount = requireNonNegative(totalRowCount, "totalRowCount");
        this.publishedRowCount = requireNonNegative(publishedRowCount, "publishedRowCount");
        this.omittedRowCount = requireNonNegative(omittedRowCount, "omittedRowCount");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        this.supersededAt = Objects.requireNonNull(supersededAt, "supersededAt must not be null");
        this.previousPublishedImportId = Objects.requireNonNull(
                previousPublishedImportId, "previousPublishedImportId must not be null");
        this.failureCode = Objects.requireNonNull(failureCode, "failureCode must not be null");
    }

    public static CatalogImport published(
            CatalogImportId id,
            String schemaVersion,
            CatalogImportMetadata metadata,
            int totalRowCount,
            int publishedRowCount,
            int omittedRowCount,
            Instant publishedAt,
            Optional<CatalogImportId> previousPublishedImportId) {
        if ((long) publishedRowCount + omittedRowCount != totalRowCount) {
            throw new IllegalArgumentException("published and omitted row counts must equal totalRowCount");
        }
        return new CatalogImport(
                id,
                schemaVersion,
                metadata,
                Status.PUBLISHED,
                totalRowCount,
                publishedRowCount,
                omittedRowCount,
                publishedAt,
                Optional.of(publishedAt),
                Optional.empty(),
                previousPublishedImportId,
                Optional.empty());
    }

    public static CatalogImport rejected(
            CatalogImportId id,
            String schemaVersion,
            CatalogImportMetadata metadata,
            int totalRowCount,
            int omittedRowCount,
            String failureCode,
            Instant createdAt) {
        if (omittedRowCount > totalRowCount) {
            throw new IllegalArgumentException("omittedRowCount must not exceed totalRowCount");
        }
        return new CatalogImport(
                id,
                schemaVersion,
                metadata,
                Status.REJECTED,
                totalRowCount,
                0,
                omittedRowCount,
                createdAt,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(requireNonBlank(failureCode, "failureCode")));
    }

    public void markSuperseded(Instant at) {
        Objects.requireNonNull(at, "at must not be null");
        if (status != Status.PUBLISHED) {
            throw new IllegalStateException("only a published catalog import can be superseded");
        }
        Instant publicationInstant = publishedAt.orElseThrow();
        if (at.isBefore(publicationInstant)) {
            throw new IllegalArgumentException("supersededAt must not be before publishedAt");
        }
        status = Status.SUPERSEDED;
        supersededAt = Optional.of(at);
    }

    public CatalogImportId id() {
        return id;
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public CatalogImportMetadata metadata() {
        return metadata;
    }

    public Status status() {
        return status;
    }

    public int totalRowCount() {
        return totalRowCount;
    }

    public int publishedRowCount() {
        return publishedRowCount;
    }

    public int omittedRowCount() {
        return omittedRowCount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Optional<Instant> publishedAt() {
        return publishedAt;
    }

    public Optional<Instant> supersededAt() {
        return supersededAt;
    }

    public Optional<CatalogImportId> previousPublishedImportId() {
        return previousPublishedImportId;
    }

    public Optional<String> failureCode() {
        return failureCode;
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
