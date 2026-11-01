package com.propractix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.propractix.academicinstitution.adapter.out.observability.CatalogPublicationMetrics;
import com.propractix.academicinstitution.adapter.out.observability.MeteredInstitutionSearch;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutionsResult;
import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class CatalogObservabilityIntegrationTest {
    private static final String PATH = "/api/v1/academic-institutions";
    @Autowired MeterRegistry registry;
    @Autowired JdbcTemplate jdbc;
    @Autowired PublishInstitutionCatalog publisher;
    @Autowired MockMvc mvc;

    @BeforeEach
    void clearSyntheticDatabase() {
        jdbc.execute("TRUNCATE platform_rate_limit_bucket, academicinstitution_institution, academicinstitution_catalog_import CASCADE");
    }

    @Test
    void ageIsUnknownWithoutSnapshotAndMeasuresRetrievalAgeAcrossRepublicationAndStorageFailure() {
        var age = registry.get("propractix_catalog_snapshot_age_seconds").gauge();
        assertThat(age.value()).isNaN();
        SyntheticCatalog.publish(publisher, "Universidad Sintética Antigua");
        jdbc.update("UPDATE academicinstitution_catalog_import SET retrieved_at = CURRENT_TIMESTAMP - interval '2 hours'");
        assertThat(age.value()).isBetween(7200.0, 7205.0);
        SyntheticCatalog.publish(publisher, "Universidad Sintética Nueva");
        // A new publication's retrieved_at is authoritative, not the previous age cached in a JVM.
        double expected = jdbc.queryForObject("SELECT extract(epoch FROM CURRENT_TIMESTAMP - retrieved_at) "
                + "FROM academicinstitution_catalog_import WHERE status='PUBLISHED'", Double.class);
        assertThat(age.value()).isBetween(expected, expected + 5);
        jdbc.execute("ALTER TABLE academicinstitution_catalog_import RENAME TO e8_unavailable_catalog");
        try { assertThat(age.value()).isNaN(); }
        finally { jdbc.execute("ALTER TABLE e8_unavailable_catalog RENAME TO academicinstitution_catalog_import"); }
        assertThat(age.value()).isFinite();
    }

    @Test
    void s0CountersHaveExactApprovedLabelsAndSearchDurationDoesNotCountRejectedAdmissions(CapturedOutput output) throws Exception {
        SyntheticCatalog.publish(publisher, "Universidad Sintética");
        var allowed = registry.get("propractix_rate_limit_allowed_total").counter();
        var found = registry.get("propractix_catalog_search_duration").tag("outcome", "found").timer();
        var rejected = registry.get("propractix_rate_limit_rejected_total").tag("rule_id", "burst").counter();
        double allowedBefore = allowed.count();
        long foundBefore = found.count();
        double rejectedBefore = rejected.count();
        mvc.perform(get(PATH).queryParam("query", "PRIVATE-QUERY-CANARY")).andExpect(status().isOk());
        jdbc.update("UPDATE platform_rate_limit_bucket SET request_count=15, "
                + "window_started_at=to_timestamp(floor(extract(epoch FROM CURRENT_TIMESTAMP)/10)*10) WHERE rule_id='burst'");
        mvc.perform(get(PATH)).andExpect(status().isTooManyRequests());
        assertThat(allowed.count()).isEqualTo(allowedBefore + 1);
        assertThat(found.count()).isEqualTo(foundBefore + 1);
        assertThat(rejected.count()).isEqualTo(rejectedBefore + 1);
        var errors = registry.get("propractix_rate_limit_adapter_errors_total").tag("error_class", "storage").counter();
        double errorsBefore = errors.count();
        jdbc.execute("ALTER TABLE platform_rate_limit_bucket RENAME TO e8_unavailable_bucket");
        try { mvc.perform(get(PATH)).andExpect(status().isServiceUnavailable()); }
        finally { jdbc.execute("ALTER TABLE e8_unavailable_bucket RENAME TO platform_rate_limit_bucket"); }
        assertThat(errors.count()).isEqualTo(errorsBefore + 1);
        assertThat(found.count()).isEqualTo(foundBefore + 1);
        for (var meter : registry.getMeters()) {
            var id = meter.getId();
            if (!id.getName().startsWith("propractix_")) continue;
            var keys = id.getTags().stream().map(tag -> tag.getKey()).toList();
            switch (id.getName()) {
                case "propractix_rate_limit_allowed_total" -> assertThat(keys).containsExactly("operation_id", "policy_version");
                case "propractix_rate_limit_rejected_total" -> assertThat(keys).containsExactly("operation_id", "policy_version", "rule_id");
                case "propractix_rate_limit_adapter_errors_total" -> {
                    assertThat(keys).containsExactly("error_class", "operation_id", "policy_version");
                    assertThat(id.getTag("error_class")).isIn("storage", "configuration", "key", "client_ip");
                }
                case "propractix_catalog_search_duration" -> assertThat(keys).containsExactly("outcome");
                case "propractix_catalog_snapshot_age_seconds" -> assertThat(keys).isEmpty();
                default -> throw new AssertionError("Unreviewed operational metric: " + id.getName());
            }
            assertThat(id.toString()).doesNotContain("PRIVATE-QUERY-CANARY", "Sintética", "fingerprint", "hmac");
        }
        assertThat(output.getAll()).doesNotContain("PRIVATE-QUERY-CANARY");
        // HTTP remains health-only for management; no new public metric surface.
        mvc.perform(get("/actuator/metrics")).andExpect(status().isForbidden());
    }

    @Test
    void durationRecordsUnavailableConflictAndUnexpectedFailureWithoutExceptionOrRequestLabels() {
        var clock = new MockClock();
        var meters = new SimpleMeterRegistry(io.micrometer.core.instrument.simple.SimpleConfig.DEFAULT, clock);
        try {
            var query = new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 20);
            var outcomes = List.of(new SearchPublishedInstitutionsResult.SnapshotChanged(),
                    new SearchPublishedInstitutionsResult.CatalogUnavailable(SearchPublishedInstitutionsResult.Failure.READ_FAILED));
            for (var result : outcomes) {
                var decorated = new MeteredInstitutionSearch(value -> { clock.add(Duration.ofMillis(25)); return result; }, meters);
                assertThat(decorated.search(query)).isSameAs(result);
            }
            var failing = new MeteredInstitutionSearch(value -> {
                clock.add(Duration.ofMillis(40)); throw new IllegalStateException("PRIVATE-EXCEPTION-CANARY");
            }, meters);
            assertThatThrownBy(() -> failing.search(query)).isInstanceOf(IllegalStateException.class);
            for (String outcome : List.of("snapshot_changed", "unavailable", "failed")) {
                var timer = meters.get("propractix_catalog_search_duration").tag("outcome", outcome).timer();
                assertThat(timer.count()).isEqualTo(1);
                assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(outcome.equals("failed") ? 40 : 25);
            }
            assertThat(meters.getMeters().toString()).doesNotContain("PRIVATE-EXCEPTION-CANARY");
        } finally { meters.close(); }
    }

    @Test
    void publicationAttemptsCountBothSuccessAndFailuresWithoutUnboundedLabels(CapturedOutput output) {
        var meters = new SimpleMeterRegistry();
        try {
            var publication = new CatalogPublicationMetrics(meters);
            publication.completed(0);
            publication.completed(4);
            publication.completed(6);
            assertThat(meters.get("propractix_catalog_publication_attempts_total").tag("outcome", "success").counter().count()).isEqualTo(1);
            assertThat(meters.get("propractix_catalog_publication_attempts_total").tag("outcome", "failure").counter().count()).isEqualTo(2);
            assertThat(meters.getMeters()).hasSize(2);
            meters.getMeters().forEach(meter -> assertThat(meter.getId().getTags()).hasSize(1));
            assertThat(output.getAll()).contains("success", "failure").doesNotContain("artifact", "sourceRecordId");
        } finally { meters.close(); }
    }
}
