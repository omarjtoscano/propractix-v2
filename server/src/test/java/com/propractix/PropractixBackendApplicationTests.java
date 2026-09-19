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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropractixBackendApplicationTests {
    private final MockMvcTester mockMvc;

    @Autowired
    PropractixBackendApplicationTests(MockMvcTester mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void contextLoads() {
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
