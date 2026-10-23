package com.propractix.platform.ratelimit.adapter.out.persistence;

import com.propractix.platform.ratelimit.application.model.ClientIpFingerprints;
import com.propractix.platform.ratelimit.application.model.RateLimitDecision;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import com.propractix.platform.ratelimit.application.model.RateLimitBacklog;
import com.propractix.platform.ratelimit.application.port.out.RateLimitBucketMaintenance;
import com.propractix.platform.ratelimit.application.port.out.RateLimitBucketStore;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/** Independent, short JDBC transactions; neither caller transactions nor JVM time control quotas. */
@Component
public final class PostgresRateLimitBucketStore implements RateLimitBucketStore, RateLimitBucketMaintenance {
    private final DataSource dataSource;

    public PostgresRateLimitBucketStore(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public RateLimitDecision claim(RateLimitPolicy policy, ClientIpFingerprints fingerprints) {
        if (fingerprints == null || !fingerprints.versions().equals(Set.copyOf(policy.hmac().readVersions()))) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.KEY);
        }
        int expectedRows = policy.operation().rules().size() * fingerprints.versions().size();
        String values = String.join(",", Collections.nCopies(expectedRows,
                "(?::text, ?::integer, ?::text, ?::integer, ?::integer, ?::integer, ?::text, ?::text)"));
        String sql = """
                WITH input(operation_id, policy_version, rule_id, quota, window_seconds, ttl_seconds,
                           hmac_key_version, subject_fingerprint) AS (VALUES %s),
                claimed AS (
                    INSERT INTO platform_rate_limit_bucket AS bucket
                        (operation_id, policy_version, rule_id, hmac_key_version, subject_fingerprint,
                         window_started_at, request_count, expires_at)
                    SELECT operation_id, policy_version, rule_id, hmac_key_version, subject_fingerprint,
                           to_timestamp(floor(extract(epoch FROM CURRENT_TIMESTAMP) / window_seconds) * window_seconds),
                           1,
                           to_timestamp(floor(extract(epoch FROM CURRENT_TIMESTAMP) / window_seconds) * window_seconds)
                               + make_interval(secs => ttl_seconds)
                      FROM input
                     ORDER BY operation_id, policy_version, rule_id, hmac_key_version, subject_fingerprint
                    ON CONFLICT (operation_id, policy_version, rule_id, hmac_key_version,
                                 subject_fingerprint, window_started_at)
                    DO UPDATE SET request_count = CASE WHEN bucket.expires_at <= CURRENT_TIMESTAMP
                                                      THEN 1 ELSE bucket.request_count + 1 END,
                                  expires_at = EXCLUDED.expires_at
                    RETURNING rule_id, hmac_key_version, request_count, window_started_at
                )
                SELECT claimed.rule_id, request_count > quota AS violated,
                       greatest(1, ceil(extract(epoch FROM
                           (window_started_at + make_interval(secs => window_seconds) - CURRENT_TIMESTAMP))))::bigint AS retry
                  FROM claimed JOIN input USING (rule_id, hmac_key_version)
                """.formatted(values);
        return transaction(connection -> {
            Set<String> violated = new HashSet<>();
            long retry = 0;
            int rows = 0;
            try (var statement = connection.prepareStatement(sql)) {
                int index = 1;
                for (var rule : policy.operation().rules()) {
                    for (String version : policy.hmac().readVersions()) {
                        statement.setString(index++, policy.operation().operationId());
                        statement.setInt(index++, policy.version());
                        statement.setString(index++, rule.ruleId());
                        statement.setInt(index++, rule.limit());
                        statement.setInt(index++, Math.toIntExact(rule.window().toSeconds()));
                        statement.setInt(index++, Math.toIntExact(rule.counterTtl().toSeconds()));
                        statement.setString(index++, version);
                        statement.setString(index++, fingerprints.get(version).value());
                    }
                }
                try (var result = statement.executeQuery()) {
                    while (result.next()) {
                        rows++;
                        if (result.getBoolean("violated")) {
                            violated.add(result.getString("rule_id"));
                            retry = Math.max(retry, result.getLong("retry"));
                        }
                    }
                }
            }
            if (rows != expectedRows) throw new SQLException();
            var decision = new RateLimitDecision(violated.isEmpty(), Duration.ofSeconds(retry), violated);
            if (decision.allowed()) connection.commit();
            else connection.rollback();
            return decision;
        });
    }

    /** Returns only aggregates; identifiers never leave this adapter. */
    @Override
    public RateLimitBacklog backlog() {
        return transaction(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT count(*) AS expired,
                           coalesce(greatest(0, floor(extract(epoch FROM
                               (CURRENT_TIMESTAMP - min(expires_at)))))::bigint, 0) AS oldest_seconds
                      FROM platform_rate_limit_bucket WHERE expires_at <= CURRENT_TIMESTAMP
                    """); var result = statement.executeQuery()) {
                result.next();
                var backlog = new RateLimitBacklog(result.getLong("expired"), Duration.ofSeconds(result.getLong("oldest_seconds")));
                connection.commit();
                return backlog;
            }
        });
    }

    @Override
    public int deleteExpiredBatch(int batchSize) {
        if (batchSize < 1 || batchSize > 1000) throw new IllegalArgumentException("Invalid cleanup batch size");
        return transaction(connection -> {
            try (var statement = connection.prepareStatement("""
                    WITH expired AS (
                        SELECT ctid FROM platform_rate_limit_bucket
                         WHERE expires_at <= CURRENT_TIMESTAMP ORDER BY expires_at
                         LIMIT ? FOR UPDATE SKIP LOCKED
                    )
                    DELETE FROM platform_rate_limit_bucket WHERE ctid IN (SELECT ctid FROM expired)
                    """)) {
                statement.setInt(1, batchSize);
                int deleted = statement.executeUpdate();
                connection.commit();
                return deleted;
            }
        });
    }

    /** Checks the migrated schema and every required privilege without consuming a quota. */
    @Override
    public void verifyAvailable() {
        transaction(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT has_table_privilege(current_user, 'platform_rate_limit_bucket', 'SELECT')
                       AND has_table_privilege(current_user, 'platform_rate_limit_bucket', 'INSERT')
                       AND has_table_privilege(current_user, 'platform_rate_limit_bucket', 'UPDATE')
                       AND has_table_privilege(current_user, 'platform_rate_limit_bucket', 'DELETE') AS available,
                       (SELECT count(*) FROM (SELECT operation_id, policy_version, rule_id, hmac_key_version,
                           subject_fingerprint, window_started_at, request_count, expires_at
                           FROM platform_rate_limit_bucket LIMIT 0) AS schema_probe)
                    """); var result = statement.executeQuery()) {
                if (!result.next() || !result.getBoolean("available")) throw new SQLException();
                connection.commit();
                return null;
            }
        });
    }

    private <T> T transaction(SqlWork<T> work) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                try (var statement = connection.createStatement()) {
                    statement.execute("SET LOCAL statement_timeout = '2s'");
                    statement.execute("SET LOCAL lock_timeout = '1s'");
                }
                return work.run(connection);
            } catch (SQLException | RuntimeException failure) {
                try { connection.rollback(); } catch (SQLException ignored) { /* Preserve safe failure below. */ }
                throw new RateLimitFailure(RateLimitFailure.ErrorClass.STORAGE);
            }
        } catch (SQLException | RuntimeException failure) {
            // JDBC/transaction exceptions may contain SQL, bind parameters or connection credentials.
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.STORAGE);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> { T run(Connection connection) throws SQLException; }

}
