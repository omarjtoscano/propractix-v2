package com.propractix.platform.ratelimit.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.platform.ratelimit.adapter.in.maintenance.RateLimitCleanupJob;
import com.propractix.platform.ratelimit.adapter.out.crypto.HmacClientIpFingerprint;
import com.propractix.platform.ratelimit.adapter.out.observability.MeteredRateLimitAdmission;
import com.propractix.platform.ratelimit.adapter.out.observability.RateLimitHealthIndicator;
import com.propractix.platform.ratelimit.application.model.ClientIpFingerprints;
import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitDecision;
import com.propractix.platform.ratelimit.application.model.RateLimitBacklog;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import com.propractix.platform.ratelimit.application.service.RateLimitAdmissionService;
import com.propractix.platform.ratelimit.configuration.CanonicalRateLimitPolicyLoader;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class RateLimitPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    static final RateLimitPolicy CANONICAL = new CanonicalRateLimitPolicyLoader().policy();
    static final ClientNetworkSubject SUBJECT = new ClientNetworkSubject(new byte[]{(byte) 192, 0, 2, 9});
    static final String SENSITIVE = "DO-NOT-LOG jdbc:postgresql://private.invalid password=secret 192.0.2.9";
    static DataSource dataSource;
    static JdbcTemplate jdbc;
    PostgresRateLimitBucketStore store;

    @BeforeAll
    static void migratesFromV1ToV2AndValidatesChecksums() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).target("1").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        // The applied V1 checksum remains unchanged by the forward migration.
        String checksum = jdbc.queryForObject("SELECT checksum::text FROM flyway_schema_history WHERE version = '1'", String.class);
        var flyway = Flyway.configure().dataSource(dataSource).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();
        assertThat(jdbc.queryForObject("SELECT checksum::text FROM flyway_schema_history WHERE version = '1'", String.class))
                .isEqualTo(checksum);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @BeforeEach
    void reset() {
        jdbc.execute("DROP TRIGGER IF EXISTS injected_failure ON platform_rate_limit_bucket");
        jdbc.execute("TRUNCATE platform_rate_limit_bucket");
        store = new PostgresRateLimitBucketStore(dataSource);
    }

    @Test
    void schemaHasOnlyApprovedEphemeralColumnsAndRejectsInvalidValues() {
        assertThat(jdbc.queryForObject("SELECT current_setting('server_version_num')::integer / 10000", Integer.class)).isEqualTo(16);
        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_name = 'platform_rate_limit_bucket' ORDER BY ordinal_position
                """, String.class)).containsExactly("operation_id", "policy_version", "rule_id", "hmac_key_version",
                "subject_fingerprint", "window_started_at", "request_count", "expires_at");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE tablename = 'platform_rate_limit_bucket'", Integer.class))
                .isEqualTo(2);
        assertThat(store.claim(CANONICAL, fingerprints(CANONICAL)).allowed()).isTrue();
        assertThatThrownBy(() -> jdbc.update("UPDATE platform_rate_limit_bucket SET request_count = 0")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE platform_rate_limit_bucket SET subject_fingerprint = '192.0.2.9'"))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE platform_rate_limit_bucket SET expires_at = window_started_at"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void derivesFixedWindowsAndExactTtlsFromPostgresClockAndCanonicalPolicy() {
        assertThat(store.claim(CANONICAL, fingerprints(CANONICAL))).isEqualTo(new RateLimitDecision(true, Duration.ZERO, java.util.Set.of()));
        for (var rule : CANONICAL.operation().rules()) {
            assertThat(jdbc.queryForObject("""
                    SELECT extract(epoch FROM window_started_at)::bigint %% ? = 0
                       AND window_started_at <= CURRENT_TIMESTAMP
                       AND window_started_at + make_interval(secs => ?) > CURRENT_TIMESTAMP
                       AND expires_at = window_started_at + make_interval(secs => ?)
                    FROM platform_rate_limit_bucket WHERE rule_id = ?
                    """.replace("%%", "%"), Boolean.class, rule.window().toSeconds(), rule.window().toSeconds(),
                    rule.counterTtl().toSeconds(), rule.ruleId())).isTrue();
        }
    }

    @Test
    void firstBurstRejectionDoesNotConsumeSustainedOrAnyVersion() {
        var policy = stablePolicy(List.of("rl-ip-v1", "rl-ip-v2"));
        int limit = rule(policy, "burst").limit();
        for (int i = 0; i < limit; i++) assertThat(store.claim(policy, fingerprints(policy)).allowed()).isTrue();
        var denied = store.claim(policy, fingerprints(policy));
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.violatedRuleIds()).containsExactly("burst");
        assertThat(denied.retryAfter().toSeconds()).isPositive().isLessThanOrEqualTo(rule(policy, "burst").window().toSeconds());
        assertThat(counts()).hasSize(4).allMatch(count -> count == limit);
        assertThat(store.claim(policy, fingerprints(policy)).violatedRuleIds()).isEqualTo(denied.violatedRuleIds());
        assertThat(counts()).allMatch(count -> count == limit);
    }

    @Test
    void sustainedFirstRejectionRollsBackOtherRuleAndRecoversByWindowWithoutDeletingRows() {
        var policy = stablePolicy(List.of("rl-ip-v1"));
        int limit = rule(policy, "sustained").limit();
        for (int i = 0; i < limit; i++) {
            if (i > 0) ageRule("burst");
            assertThat(store.claim(policy, fingerprints(policy)).allowed()).isTrue();
        }
        ageRule("burst");
        long rows = rowCount();
        var denied = store.claim(policy, fingerprints(policy));
        assertThat(denied.violatedRuleIds()).containsExactly("sustained");
        assertThat(rowCount()).isEqualTo(rows); // New burst row was rolled back, too.
        ageRule("sustained");
        assertThat(store.claim(policy, fingerprints(policy)).allowed()).isTrue();
        assertThat(rowCount()).isGreaterThan(rows); // Old rows remain physically present.
    }

    @Test
    void reportsEveryViolatedRuleAndMaximumCeiledRetryAfter() {
        var policy = stablePolicy(List.of("rl-ip-v1"));
        store.claim(policy, fingerprints(policy));
        for (var rule : policy.operation().rules()) {
            jdbc.update("UPDATE platform_rate_limit_bucket SET request_count = ? WHERE rule_id = ?", rule.limit(), rule.ruleId());
        }
        var denied = store.claim(policy, fingerprints(policy));
        assertThat(denied.violatedRuleIds()).containsExactlyInAnyOrder("burst", "sustained");
        long max = jdbc.queryForObject("""
                SELECT ceil(max(extract(epoch FROM (window_started_at + make_interval(secs =>
                     CASE rule_id WHEN 'burst' THEN ? ELSE ? END) - CURRENT_TIMESTAMP))))::bigint
                FROM platform_rate_limit_bucket
                """, Long.class, rule(policy, "burst").window().toSeconds(), rule(policy, "sustained").window().toSeconds());
        assertThat(denied.retryAfter().toSeconds()).isBetween(max, max + 1);
    }

    @Test
    void concurrentClaimsNeverOverbookAndAllActiveVersionsHaveSameCommittedCount() throws Exception {
        var policy = stablePolicy(List.of("rl-ip-v2", "rl-ip-v1"));
        var fingerprints = fingerprints(policy);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RateLimitDecision>> results = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(24)) {
            for (int i = 0; i < 48; i++) results.add(executor.submit(() -> {
                start.await();
                return store.claim(policy, fingerprints);
            }));
            start.countDown();
            int admitted = 0;
            for (var result : results) if (result.get(20, TimeUnit.SECONDS).allowed()) admitted++;
            assertThat(admitted).isEqualTo(rule(policy, "burst").limit());
            assertThat(counts()).hasSize(4).allMatch(count -> count == rule(policy, "burst").limit());
        }
    }

    @Test
    void rotationPreservesOldQuotaAndRollsBackNewVersionOnRejection() {
        var old = stablePolicy(List.of("rl-ip-v1"));
        var overlap = stablePolicy(List.of("rl-ip-v1", "rl-ip-v2"));
        int limit = rule(old, "burst").limit();
        for (int i = 0; i < limit; i++) store.claim(old, fingerprints(old));
        assertThat(store.claim(overlap, fingerprints(overlap)).allowed()).isFalse();
        assertThat(rowCount()).isEqualTo(2); // No permissive new-version counters committed.
        jdbc.update("UPDATE platform_rate_limit_bucket SET request_count = request_count - 1");
        assertThat(store.claim(overlap, fingerprints(overlap)).allowed()).isTrue();
        assertThat(store.claim(overlap, fingerprints(overlap)).allowed()).isFalse();
        assertThat(jdbc.queryForList("SELECT request_count FROM platform_rate_limit_bucket WHERE hmac_key_version = 'rl-ip-v2'", Integer.class))
                .containsOnly(1);
        // Controlled rows model completion of the overlap horizon before retiring the old key.
        ageRule("burst");
        ageRule("sustained");
        var next = stablePolicy(List.of("rl-ip-v2"));
        assertThat(store.claim(next, fingerprints(next)).allowed()).isTrue();
    }

    @Test
    void missingOrExtraVersionFailsBeforeStorageAndSecretsNeverReachDatabase() {
        var policy = stablePolicy(List.of("rl-ip-v1", "rl-ip-v2"));
        assertThatThrownBy(() -> store.claim(policy, fingerprints(stablePolicy(List.of("rl-ip-v1")))))
                .isInstanceOf(RateLimitFailure.class).hasMessage("rate_limiter_unavailable:key").hasNoCause();
        assertThatThrownBy(() -> store.claim(CANONICAL, fingerprints(policy))).hasMessage("rate_limiter_unavailable:key");
        assertThat(rowCount()).isZero();
    }

    @Test
    void logicalExpirationIgnoresExpiredCountersEvenBeforePhysicalCleanup() {
        var policy = stablePolicy(List.of("rl-ip-v1"));
        store.claim(policy, fingerprints(policy));
        for (var rule : policy.operation().rules()) {
            jdbc.update("""
                    UPDATE platform_rate_limit_bucket SET request_count = ?, expires_at = CURRENT_TIMESTAMP - interval '1 microsecond'
                    WHERE rule_id = ?
                    """, rule.limit(), rule.ruleId());
        }
        assertThat(store.claim(policy, fingerprints(policy)).allowed()).isTrue();
        assertThat(counts()).containsOnly(1);
    }

    @Test
    void canonicalTtlBoundariesAreLogicallyExpiredAndDeletedInTheNextCycle() {
        store.claim(CANONICAL, fingerprints(CANONICAL));
        for (var rule : CANONICAL.operation().rules()) {
            // SQL controls the boundary: expires_at equals database now, without a JVM clock or sleeps.
            jdbc.update("""
                    UPDATE platform_rate_limit_bucket
                       SET window_started_at = CURRENT_TIMESTAMP - make_interval(secs => ?),
                           expires_at = CURRENT_TIMESTAMP, request_count = ?
                     WHERE rule_id = ?
                    """, rule.counterTtl().toSeconds(), rule.limit(), rule.ruleId());
        }
        assertThat(store.backlog().expiredRows()).isEqualTo(CANONICAL.operation().rules().size());
        assertThat(store.claim(CANONICAL, fingerprints(CANONICAL)).allowed()).isTrue();
        var registry = new SimpleMeterRegistry();
        new RateLimitCleanupJob(store, CANONICAL, metered(CANONICAL, store, registry)::error).clean();
        assertThat(store.backlog().expiredRows()).isZero();
        assertThat(rowCount()).isEqualTo(CANONICAL.operation().rules().size());
        assertThat(counts()).containsOnly(1);
    }

    @Test
    void cleanupDeletesOnlyExpiredRowsInBoundedIdempotentBatchesAndAlertsOnBacklog(CapturedOutput output) {
        store.claim(CANONICAL, fingerprints(CANONICAL));
        long threshold = CANONICAL.cleanup().expiredRowIncidentThreshold();
        jdbc.update("""
                INSERT INTO platform_rate_limit_bucket
                SELECT operation_id, policy_version, rule_id, hmac_key_version, subject_fingerprint,
                       CURRENT_TIMESTAMP - interval '2 days' - n * interval '1 second', 1,
                       CURRENT_TIMESTAMP - interval '1 second'
                FROM platform_rate_limit_bucket CROSS JOIN generate_series(1, ?) n WHERE rule_id = 'burst'
                """, threshold + 1);
        assertThat(store.backlog().incident(CANONICAL.cleanup())).isTrue();
        assertThat(new RateLimitHealthIndicator(store, CANONICAL).health().getStatus().getCode()).isEqualTo("DOWN");
        assertThat(store.deleteExpiredBatch(3)).isEqualTo(3);
        // Also exercise the independent age threshold, regardless of row count.
        jdbc.update("UPDATE platform_rate_limit_bucket SET expires_at = CURRENT_TIMESTAMP - make_interval(secs => ?) WHERE expires_at < CURRENT_TIMESTAMP",
                CANONICAL.cleanup().oldestExpiredIncidentThreshold().plusSeconds(1).toSeconds());
        var registry = new SimpleMeterRegistry();
        var metrics = metered(CANONICAL, store, registry);
        var cleanup = new RateLimitCleanupJob(store, CANONICAL, metrics::error);
        cleanup.clean();
        cleanup.clean();
        assertThat(store.backlog().expiredRows()).isZero();
        assertThat(rowCount()).isEqualTo(2);
        assertThat(new RateLimitHealthIndicator(store, CANONICAL).health().getStatus().getCode()).isEqualTo("UP");
        assertThat(output.getAll()).contains("rate_limit_cleanup_incident", "rate_limit_cleanup_completed")
                .doesNotContain("192.0.2.9", fingerprints(CANONICAL).get("rl-ip-v1").value(), "rl-ip-v1");
        assertThat(new RateLimitBacklog(1,
                CANONICAL.cleanup().oldestExpiredIncidentThreshold().plusSeconds(1)).incident(CANONICAL.cleanup())).isTrue();
        assertThat(new RateLimitBacklog(threshold,
                CANONICAL.cleanup().oldestExpiredIncidentThreshold()).incident(CANONICAL.cleanup())).isFalse();
    }

    @Test
    void sqlFailureAfterPartialWritesAndCommitFailureRollbackWithOnlySafeDiagnostics(CapturedOutput output) {
        var policy = stablePolicy(List.of("rl-ip-v1", "rl-ip-v2"));
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION rate_limit_injected_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.rule_id = 'sustained' THEN RAISE EXCEPTION 'DO-NOT-LOG 192.0.2.9'; END IF;
                    RETURN NEW;
                END $$
                """);
        jdbc.execute("CREATE TRIGGER injected_failure BEFORE INSERT ON platform_rate_limit_bucket FOR EACH ROW EXECUTE FUNCTION rate_limit_injected_failure()");
        assertStorageFailure(() -> store.claim(policy, fingerprints(policy)));
        assertThat(rowCount()).isZero();
        jdbc.execute("DROP TRIGGER injected_failure ON platform_rate_limit_bucket");
        var commitFailure = new PostgresRateLimitBucketStore(faultyDataSource("commit"));
        assertStorageFailure(() -> commitFailure.claim(policy, fingerprints(policy)));
        assertThat(rowCount()).isZero();
        assertThat(output.getAll()).doesNotContain("DO-NOT-LOG", "192.0.2.9", "rl-ip-v2");
    }

    @Test
    void connectionAndTransactionFailuresCloseAdmissionHealthAndCleanupWithoutLeakingCauses(CapturedOutput output) {
        for (String method : List.of("getConnection", "setAutoCommit", "setTransactionIsolation")) {
            var broken = new PostgresRateLimitBucketStore(faultyDataSource(method));
            assertStorageFailure(() -> broken.claim(CANONICAL, fingerprints(CANONICAL)));
            var health = new RateLimitHealthIndicator(broken, CANONICAL).health();
            assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
            assertThat(health.getDetails()).isEmpty();
            var registry = new SimpleMeterRegistry();
            new RateLimitCleanupJob(broken, CANONICAL, metered(CANONICAL, broken, registry)::error).clean();
            assertThat(registry.get("propractix_rate_limit_adapter_errors_total").tag("error_class", "storage").counter().count())
                    .isEqualTo(1);
        }
        assertThat(output.getAll()).doesNotContain("DO-NOT-LOG", "192.0.2.9", "password=secret", "private.invalid");
        assertThat(rowCount()).isZero();
    }

    @Test
    void lockTimeoutRollsBackAndReturnsSafeStorageFailure() throws Exception {
        var policy = stablePolicy(List.of("rl-ip-v1"));
        store.claim(policy, fingerprints(policy));
        try (var lock = dataSource.getConnection()) {
            lock.setAutoCommit(false);
            try (var statement = lock.createStatement()) {
                statement.execute("SELECT * FROM platform_rate_limit_bucket FOR UPDATE");
            }
            assertStorageFailure(() -> store.claim(policy, fingerprints(policy)));
            lock.rollback();
        }
        assertThat(counts()).containsOnly(1);
    }

    @Test
    void concurrentCleanupSkipsLockedExpiredRowsAndNeverDeletesActiveQuotas() throws Exception {
        store.claim(CANONICAL, fingerprints(CANONICAL));
        jdbc.update("""
                INSERT INTO platform_rate_limit_bucket
                SELECT operation_id, policy_version, rule_id, hmac_key_version, subject_fingerprint,
                       window_started_at - interval '1 day', request_count, expires_at - interval '1 day'
                FROM platform_rate_limit_bucket
                """);
        try (var lock = dataSource.getConnection()) {
            lock.setAutoCommit(false);
            try (var statement = lock.createStatement()) {
                statement.execute("SELECT * FROM platform_rate_limit_bucket WHERE expires_at <= CURRENT_TIMESTAMP FOR UPDATE");
            }
            assertThat(store.deleteExpiredBatch(1000)).isZero();
            assertThat(store.claim(CANONICAL, fingerprints(CANONICAL)).allowed()).isTrue();
            lock.rollback();
        }
        assertThat(store.deleteExpiredBatch(1000)).isEqualTo(2);
        assertThat(store.deleteExpiredBatch(1000)).isZero();
        assertThat(counts()).containsOnly(2);
    }

    @Test
    void missingMigratedTableMakesReadinessAndAdmissionFailClosedWithoutDetails() {
        jdbc.execute("ALTER TABLE platform_rate_limit_bucket RENAME TO platform_rate_limit_bucket_unavailable");
        try {
            assertStorageFailure(() -> store.claim(CANONICAL, fingerprints(CANONICAL)));
            var health = new RateLimitHealthIndicator(store, CANONICAL).health();
            assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
            assertThat(health.getDetails()).isEmpty();
        } finally {
            jdbc.execute("ALTER TABLE platform_rate_limit_bucket_unavailable RENAME TO platform_rate_limit_bucket");
        }
        assertThat(new RateLimitHealthIndicator(store, CANONICAL).health().getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void metricsAndDatabaseContainOnlyApprovedLabelsAndHmacPseudonyms(CapturedOutput output) {
        var policy = stablePolicy(List.of("rl-ip-v1"));
        var registry = new SimpleMeterRegistry();
        var admission = metered(policy, store, registry);
        int limit = rule(policy, "burst").limit();
        for (int i = 0; i < limit; i++) admission.admit(policy.operation().operationId(), SUBJECT);
        admission.admit(policy.operation().operationId(), SUBJECT);
        assertThatThrownBy(() -> admission.admit("192.0.2.9", SUBJECT)).hasMessage("rate_limiter_unavailable:configuration");
        assertThatThrownBy(() -> admission.admit(policy.operation().operationId(), null)).hasMessage("rate_limiter_unavailable:client_ip");
        assertThat(registry.get("propractix_rate_limit_allowed_total").counter().count()).isEqualTo(limit);
        assertThat(registry.get("propractix_rate_limit_rejected_total").tag("rule_id", "burst").counter().count()).isEqualTo(1);
        for (var meter : registry.getMeters()) {
            assertThat(meter.getId().getTags()).allSatisfy(tag -> {
                assertThat(tag.getKey()).isIn("operation_id", "policy_version", "rule_id", "error_class");
                assertThat(tag.getValue()).isIn(policy.operation().operationId(), "1", "burst", "sustained",
                        "storage", "configuration", "key", "client_ip");
            });
        }
        String pseudonym = fingerprints(policy).get("rl-ip-v1").value();
        assertThat(jdbc.queryForList("SELECT DISTINCT subject_fingerprint FROM platform_rate_limit_bucket", String.class))
                .containsExactly(pseudonym);
        assertThat(jdbc.queryForList("SELECT row_to_json(bucket)::text FROM platform_rate_limit_bucket bucket", String.class))
                .allSatisfy(row -> assertThat(row).doesNotContain("192.0.2.9", "c0000209", "SYNTHETIC", "password"));
        assertThat(output.getAll()).doesNotContain(pseudonym, "192.0.2.9", "SYNTHETIC", "rl-ip-v1");
    }

    private static RateLimitPolicy stablePolicy(List<String> versions) {
        // Long test-only windows avoid boundary flakiness during concurrency; all quotas come from S0.
        var rules = new ArrayList<RateLimitPolicy.Rule>();
        for (int i = 0; i < CANONICAL.operation().rules().size(); i++) {
            var original = CANONICAL.operation().rules().get(i);
            var window = Duration.ofDays(i + 1);
            rules.add(new RateLimitPolicy.Rule(original.ruleId(), original.limit(), window, window.plusSeconds(60)));
        }
        var op = CANONICAL.operation();
        var hmac = CANONICAL.hmac();
        return new RateLimitPolicy(CANONICAL.policyId(), CANONICAL.version(),
                new RateLimitPolicy.Operation(op.operationId(), op.method(), op.pathTemplate(), op.contractVersion(), op.excludedQueryParameters(), rules),
                new RateLimitPolicy.Hmac(hmac.algorithm(), hmac.purpose(), hmac.domainSeparator(), hmac.encoding(), versions.getLast(), versions),
                CANONICAL.cleanup());
    }

    private static HmacClientIpFingerprint crypto(RateLimitPolicy policy) {
        Map<String, byte[]> material = new HashMap<>();
        for (String version : policy.hmac().readVersions()) {
            material.put(version, ("SYNTHETIC-TEST-ONLY-KEY-NOT-FOR-DEPLOYMENT-" + version).getBytes(StandardCharsets.UTF_8));
        }
        return new HmacClientIpFingerprint(policy.hmac(), material);
    }

    private static ClientIpFingerprints fingerprints(RateLimitPolicy policy) { return crypto(policy).fingerprint(SUBJECT); }
    private static RateLimitPolicy.Rule rule(RateLimitPolicy policy, String id) {
        return policy.operation().rules().stream().filter(rule -> rule.ruleId().equals(id)).findFirst().orElseThrow();
    }
    private static MeteredRateLimitAdmission metered(RateLimitPolicy policy, PostgresRateLimitBucketStore store, SimpleMeterRegistry registry) {
        return new MeteredRateLimitAdmission(new RateLimitAdmissionService(() -> policy, crypto(policy), store), policy, registry);
    }
    private static long rowCount() { return jdbc.queryForObject("SELECT count(*) FROM platform_rate_limit_bucket", Long.class); }
    private static List<Integer> counts() { return jdbc.queryForList("SELECT request_count FROM platform_rate_limit_bucket", Integer.class); }
    private static void ageRule(String rule) {
        jdbc.update("""
                UPDATE platform_rate_limit_bucket
                SET window_started_at = window_started_at - interval '4 days'
                        - (SELECT count(*) FROM platform_rate_limit_bucket) * interval '1 second',
                    expires_at = expires_at - interval '4 days'
                        - (SELECT count(*) FROM platform_rate_limit_bucket) * interval '1 second'
                WHERE rule_id = ? AND expires_at > CURRENT_TIMESTAMP
                """, rule);
    }
    private static void assertStorageFailure(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(RateLimitFailure.class)
                .hasMessage("rate_limiter_unavailable:storage").hasNoCause().hasNoSuppressedExceptions();
    }
    private static DataSource faultyDataSource(String failureMethod) {
        return new AbstractDataSource() {
            @Override public Connection getConnection() throws SQLException {
                if (failureMethod.equals("getConnection")) throw new SQLException(SENSITIVE);
                Connection real = dataSource.getConnection();
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals(failureMethod)) throw new SQLException(SENSITIVE);
                            try { return method.invoke(real, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                        });
            }
            @Override public Connection getConnection(String username, String password) throws SQLException { return getConnection(); }
        };
    }
}
