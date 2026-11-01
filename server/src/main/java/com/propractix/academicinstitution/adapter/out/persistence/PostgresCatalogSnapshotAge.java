package com.propractix.academicinstitution.adapter.out.persistence;

import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/** Data freshness since retrieval, rather than time since a potentially recent republication. */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public final class PostgresCatalogSnapshotAge {
    private final JdbcTemplate jdbc;

    public PostgresCatalogSnapshotAge(DataSource dataSource, MeterRegistry registry) {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(2);
        Gauge.builder("propractix_catalog_snapshot_age_seconds", this, PostgresCatalogSnapshotAge::seconds)
                .description("Age of the published data since retrieved_at; NaN when unavailable")
                .register(registry);
    }

    public double seconds() {
        try {
            Double age = jdbc.queryForObject("""
                    SELECT extract(epoch FROM (CURRENT_TIMESTAMP - retrieved_at))::double precision
                      FROM academicinstitution_catalog_import WHERE status = 'PUBLISHED'
                    """, Double.class);
            return age == null ? Double.NaN : Math.max(0, age);
        } catch (DataAccessException failure) {
            // Missing snapshot/storage is unknown, never a falsely fresh zero. Do not log SQL/cause.
            return Double.NaN;
        }
    }
}
