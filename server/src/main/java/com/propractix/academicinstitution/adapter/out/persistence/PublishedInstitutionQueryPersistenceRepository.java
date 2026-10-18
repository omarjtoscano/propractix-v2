package com.propractix.academicinstitution.adapter.out.persistence;

import com.propractix.academicinstitution.application.port.out.PublishedCatalogSnapshotMetadata;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionSearchRow;
import com.propractix.academicinstitution.application.query.InstitutionSearchCursor;
import com.propractix.academicinstitution.application.query.NormalizedInstitutionSearchTerm;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class PublishedInstitutionQueryPersistenceRepository {

    private final JdbcTemplate jdbcTemplate;

    PublishedInstitutionQueryPersistenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Optional<PublishedCatalogSnapshotMetadata> findPublishedSnapshot() {
        List<PublishedCatalogSnapshotMetadata> snapshots = jdbcTemplate.query(
                """
                SELECT catalog_import_id, retrieved_at, source_url, source_attribution
                  FROM academicinstitution_catalog_import
                 WHERE status = 'PUBLISHED'
                """,
                (resultSet, rowNumber) -> new PublishedCatalogSnapshotMetadata(
                        new CatalogImportId(resultSet.getObject("catalog_import_id", UUID.class)),
                        resultSet.getTimestamp("retrieved_at").toInstant(),
                        URI.create(resultSet.getString("source_url")),
                        resultSet.getString("source_attribution")));
        if (snapshots.size() > 1) {
            throw new PersistenceMappingException("multiple published catalog snapshots");
        }
        return snapshots.stream().findFirst();
    }

    List<PublishedInstitutionSearchRow> search(
            CatalogImportId snapshotId,
            Optional<NormalizedInstitutionSearchTerm> term,
            Optional<InstitutionSearchCursor> cursor,
            int fetchLimit) {
        StringBuilder sql = new StringBuilder("""
                SELECT institution_id, official_name, country_code, search_name_normalized
                  FROM academicinstitution_institution
                 WHERE published_catalog_import_id = ?
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(snapshotId.value());
        term.ifPresent(value -> {
            sql.append(" AND search_name_normalized LIKE ? ESCAPE '\\'");
            parameters.add("%" + escapeLike(value.value()) + "%");
        });
        cursor.ifPresent(value -> {
            sql.append(" AND (search_name_normalized, institution_id) > (?, ?)");
            parameters.add(value.lastSearchNameNormalized());
            parameters.add(value.lastInstitutionId().value());
        });
        sql.append(" ORDER BY search_name_normalized, institution_id LIMIT ?");
        parameters.add(fetchLimit);

        return jdbcTemplate.query(sql.toString(), PublishedInstitutionQueryPersistenceRepository::mapRow, parameters.toArray());
    }

    private static PublishedInstitutionSearchRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PublishedInstitutionSearchRow(
                new AcademicInstitutionId(resultSet.getObject("institution_id", UUID.class)),
                new OfficialInstitutionName(resultSet.getString("official_name")),
                new CountryCode(resultSet.getString("country_code").strip()),
                resultSet.getString("search_name_normalized"));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
