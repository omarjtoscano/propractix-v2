package com.propractix;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.jdbc.core.JdbcTemplate;
import com.propractix.platform.ratelimit.application.port.in.RateLimitAdmission;
import com.propractix.platform.ratelimit.adapter.in.maintenance.RateLimitCleanupJob;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropractixBackendApplicationTests {
    private final MockMvcTester mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired RateLimitAdmission rateLimitAdmission;
    @Autowired RateLimitCleanupJob rateLimitCleanupJob;

    @Autowired
    PropractixBackendApplicationTests(MockMvcTester mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void contextLoads() {
        assertThat(rateLimitAdmission).isNotNull();
        assertThat(rateLimitCleanupJob).isNotNull();
    }

    @Test
    void exposesLivenessWithoutDetails() {
        var result = mockMvc.get().uri("/actuator/health/liveness").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("UP");
        assertThat(result).bodyText().doesNotContain(
                "components", "details", "password", "username", "jdbc");
    }

    @Test
    void exposesReadinessAgainstPostgreSqlWithoutDetails() {
        var result = mockMvc.get().uri("/actuator/health/readiness").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("UP");
        assertThat(result).bodyText().doesNotContain(
                "components", "details", "password", "username", "jdbc");
    }

    @Test
    void missingRateLimitSchemaClosesReadinessWhileLivenessRemainsUp() {
        jdbc.execute("ALTER TABLE platform_rate_limit_bucket RENAME TO platform_rate_limit_bucket_probe_test");
        try {
            var result = mockMvc.get().uri("/actuator/health/readiness").exchange();
            assertThat(result).hasStatus(503);
            assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("DOWN");
            assertThat(result).bodyText().doesNotContain("components", "details", "jdbc", "exception", "platform_rate_limit_bucket");
            assertThat(mockMvc.get().uri("/actuator/health/liveness")).hasStatusOk();
        } finally {
            jdbc.execute("ALTER TABLE platform_rate_limit_bucket_probe_test RENAME TO platform_rate_limit_bucket");
        }
        assertThat(mockMvc.get().uri("/actuator/health/readiness")).hasStatusOk();
    }

    @Test
    void preservesAValidCorrelationIdAndClearsMdc() {
        String correlationId = "foundation-test-01";

        assertThat(mockMvc.get().uri("/actuator/health/liveness")
                .header("X-Correlation-ID", correlationId))
                .hasStatusOk()
                .hasHeader("X-Correlation-ID", correlationId);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void replacesAnInvalidCorrelationId() {
        assertThat(mockMvc.get().uri("/actuator/health/liveness")
                .header("X-Correlation-ID", "contains spaces"))
                .hasStatusOk()
                .headers().satisfies(headers -> {
                    String generated = headers.getFirst("X-Correlation-ID");
                    assertThat(generated).isNotBlank().isNotEqualTo("contains spaces");
                });
    }

    @Test
    void deniesUnknownHttpSurface() {
        assertThat(mockMvc.get().uri("/api/v1/not-implemented"))
                .hasStatus(403)
                .headers().satisfies(headers ->
                        assertThat(headers.getFirst("X-Correlation-ID")).isNotBlank());
    }
}
