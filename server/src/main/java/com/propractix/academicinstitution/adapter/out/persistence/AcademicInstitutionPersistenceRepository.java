package com.propractix.academicinstitution.adapter.out.persistence;

import com.propractix.academicinstitution.domain.model.ExternalInstitutionReference;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

final class AcademicInstitutionPersistenceRepository {

    private static final int REFERENCE_QUERY_BATCH_SIZE = 500;
    private static final String COLUMNS = """
            institution_id, source_system, source_record_id, official_name,
            search_name_normalized, country_code, source_status,
            published_catalog_import_id, created_at, updated_at, version
            """;

    private final JdbcTemplate jdbcTemplate;

    AcademicInstitutionPersistenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Map<ExternalInstitutionReference, AcademicInstitutionJpaEntity> findByReferences(
            List<ExternalInstitutionReference> references) {
        Map<ExternalInstitutionReference, AcademicInstitutionJpaEntity> found = new HashMap<>();
        for (int start = 0; start < references.size(); start += REFERENCE_QUERY_BATCH_SIZE) {
            List<ExternalInstitutionReference> batch =
                    references.subList(start, Math.min(start + REFERENCE_QUERY_BATCH_SIZE, references.size()));
            String placeholders = String.join(", ", java.util.Collections.nCopies(batch.size(), "(?, ?)"));
            List<Object> parameters = new ArrayList<>(batch.size() * 2);
            batch.forEach(reference -> {
                parameters.add(reference.sourceSystem());
                parameters.add(reference.sourceRecordId());
            });
            jdbcTemplate.query(
                    "SELECT " + COLUMNS
                            + " FROM academicinstitution_institution"
                            + " WHERE (source_system, source_record_id) IN (" + placeholders + ")",
                    resultSet -> {
                        AcademicInstitutionJpaEntity entity = mapRow(resultSet, 0);
                        ExternalInstitutionReference reference =
                                new ExternalInstitutionReference(entity.sourceSystem(), entity.sourceRecordId());
                        if (found.put(reference, entity) != null) {
                            throw new PersistenceMappingException("duplicate persisted external reference");
                        }
                    },
                    parameters.toArray());
        }
        return found;
    }

    void withdrawPublishedBySnapshot(UUID snapshotId, java.time.Instant now) {
        jdbcTemplate.update(
                """
                UPDATE academicinstitution_institution
                   SET published_catalog_import_id = NULL,
                       updated_at = ?,
                       version = version + 1
                 WHERE published_catalog_import_id = ?
                """,
                Timestamp.from(now),
                snapshotId);
    }

    void updateAll(List<AcademicInstitutionJpaEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        int[] counts = jdbcTemplate.batchUpdate(
                """
                UPDATE academicinstitution_institution
                   SET official_name = ?,
                       search_name_normalized = ?,
                       country_code = ?,
                       source_status = ?,
                       published_catalog_import_id = ?,
                       updated_at = ?,
                       version = version + 1
                 WHERE institution_id = ? AND version = ?
                """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement statement, int index) throws SQLException {
                        AcademicInstitutionJpaEntity entity = entities.get(index);
                        statement.setString(1, entity.officialName());
                        statement.setString(2, entity.searchNameNormalized());
                        statement.setString(3, entity.countryCode());
                        statement.setString(4, entity.sourceStatus());
                        statement.setObject(5, entity.publishedCatalogImportId());
                        statement.setTimestamp(6, Timestamp.from(entity.updatedAt()));
                        statement.setObject(7, entity.institutionId());
                        statement.setLong(8, entity.version());
                    }

                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }
                });
        assertEveryRowChanged(counts);
    }

    void insertAll(List<AcademicInstitutionJpaEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        int[] counts = jdbcTemplate.batchUpdate(
                """
                INSERT INTO academicinstitution_institution (
                    institution_id, source_system, source_record_id, official_name,
                    search_name_normalized, country_code, source_status,
                    published_catalog_import_id, created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement statement, int index) throws SQLException {
                        AcademicInstitutionJpaEntity entity = entities.get(index);
                        statement.setObject(1, entity.institutionId());
                        statement.setString(2, entity.sourceSystem());
                        statement.setString(3, entity.sourceRecordId());
                        statement.setString(4, entity.officialName());
                        statement.setString(5, entity.searchNameNormalized());
                        statement.setString(6, entity.countryCode());
                        statement.setString(7, entity.sourceStatus());
                        statement.setObject(8, entity.publishedCatalogImportId());
                        statement.setTimestamp(9, Timestamp.from(entity.createdAt()));
                        statement.setTimestamp(10, Timestamp.from(entity.updatedAt()));
                        statement.setLong(11, entity.version());
                    }

                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }
                });
        assertEveryRowChanged(counts);
    }

    private static void assertEveryRowChanged(int[] counts) {
        for (int count : counts) {
            if (count != 1 && count != PreparedStatement.SUCCESS_NO_INFO) {
                throw new ReconciliationConflictException();
            }
        }
    }

    private static AcademicInstitutionJpaEntity mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AcademicInstitutionJpaEntity(
                resultSet.getObject("institution_id", UUID.class),
                resultSet.getString("source_system"),
                resultSet.getString("source_record_id"),
                resultSet.getString("official_name"),
                resultSet.getString("search_name_normalized"),
                resultSet.getString("country_code").strip(),
                resultSet.getString("source_status"),
                resultSet.getObject("published_catalog_import_id", UUID.class),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant(),
                resultSet.getLong("version"));
    }
}
