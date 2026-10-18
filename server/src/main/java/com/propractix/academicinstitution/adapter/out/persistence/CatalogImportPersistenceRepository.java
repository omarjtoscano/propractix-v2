package com.propractix.academicinstitution.adapter.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CatalogImportPersistenceRepository {

    private static final String COLUMNS = """
            catalog_import_id, schema_version, source_url, retrieved_at,
            source_attribution, artifact_hash, source_download_hash, status,
            total_row_count, published_row_count, omitted_row_count, failure_code,
            created_at, published_at, superseded_at, previous_published_import_id, version
            """;

    private final JdbcTemplate jdbcTemplate;

    CatalogImportPersistenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean tryAcquirePublicationLock(long lockKey) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)", Boolean.class, lockKey));
    }

    Optional<CatalogImportJpaEntity> findPublished() {
        List<CatalogImportJpaEntity> rows = jdbcTemplate.query(
                "SELECT " + COLUMNS
                        + " FROM academicinstitution_catalog_import WHERE status = 'PUBLISHED'",
                CatalogImportPersistenceRepository::mapRow);
        if (rows.size() > 1) {
            throw new PersistenceMappingException("multiple published catalog snapshots");
        }
        return rows.stream().findFirst();
    }

    void markSuperseded(CatalogImportJpaEntity entity) {
        int updated = jdbcTemplate.update(
                """
                UPDATE academicinstitution_catalog_import
                   SET status = 'SUPERSEDED', superseded_at = ?, version = version + 1
                 WHERE catalog_import_id = ? AND status = 'PUBLISHED' AND version = ?
                """,
                Timestamp.from(entity.supersededAt()),
                entity.catalogImportId(),
                entity.version());
        if (updated != 1) {
            throw new ReconciliationConflictException();
        }
    }

    void insert(CatalogImportJpaEntity entity) {
        int inserted = jdbcTemplate.update(
                """
                INSERT INTO academicinstitution_catalog_import (
                    catalog_import_id, schema_version, source_url, retrieved_at,
                    source_attribution, artifact_hash, source_download_hash, status,
                    total_row_count, published_row_count, omitted_row_count, failure_code,
                    created_at, published_at, superseded_at,
                    previous_published_import_id, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                entity.catalogImportId(),
                entity.schemaVersion(),
                entity.sourceUrl(),
                Timestamp.from(entity.retrievedAt()),
                entity.sourceAttribution(),
                entity.artifactHash(),
                entity.sourceDownloadHash(),
                entity.status(),
                entity.totalRowCount(),
                entity.publishedRowCount(),
                entity.omittedRowCount(),
                entity.failureCode(),
                Timestamp.from(entity.createdAt()),
                timestamp(entity.publishedAt()),
                timestamp(entity.supersededAt()),
                entity.previousPublishedImportId(),
                entity.version());
        if (inserted != 1) {
            throw new PersistenceMappingException("catalog import was not inserted");
        }
    }

    private static CatalogImportJpaEntity mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CatalogImportJpaEntity(
                resultSet.getObject("catalog_import_id", UUID.class),
                resultSet.getString("schema_version"),
                resultSet.getString("source_url"),
                resultSet.getTimestamp("retrieved_at").toInstant(),
                resultSet.getString("source_attribution"),
                resultSet.getString("artifact_hash").strip(),
                stripNullable(resultSet.getString("source_download_hash")),
                resultSet.getString("status"),
                resultSet.getInt("total_row_count"),
                resultSet.getInt("published_row_count"),
                resultSet.getInt("omitted_row_count"),
                resultSet.getString("failure_code"),
                resultSet.getTimestamp("created_at").toInstant(),
                toInstant(resultSet, "published_at"),
                toInstant(resultSet, "superseded_at"),
                resultSet.getObject("previous_published_import_id", UUID.class),
                resultSet.getLong("version"));
    }

    private static java.time.Instant toInstant(ResultSet resultSet, String column) throws SQLException {
        java.sql.Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static String stripNullable(String value) {
        return value == null ? null : value.strip();
    }

    private static Timestamp timestamp(java.time.Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
