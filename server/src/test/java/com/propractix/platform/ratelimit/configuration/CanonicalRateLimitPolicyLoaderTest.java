package com.propractix.platform.ratelimit.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalRateLimitPolicyLoaderTest {
    private final CanonicalRateLimitPolicyLoader loader = new CanonicalRateLimitPolicyLoader();

    @Test
    void packagesTheCanonicalArtifactByteForByteAndDerivesQuotasAndTtlsFromIt() throws IOException {
        byte[] canonical = Files.readAllBytes(Path.of("../docs/policies/public-endpoint-rate-limits-v1.yaml"));
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(CanonicalRateLimitPolicyLoader.RESOURCE)) {
            assertThat(resource.readAllBytes()).isEqualTo(canonical);
        }
        // Expectations originate in the canonical policy, not an independently maintained list of limits.
        Map<?, ?> document = new org.yaml.snakeyaml.Yaml().load(new ByteArrayInputStream(canonical));
        Map<?, ?> spec = (Map<?, ?>) document.get("spec");
        Map<?, ?> operation = (Map<?, ?>) ((java.util.List<?>) spec.get("operations")).getFirst();
        java.util.List<?> sourceRules = (java.util.List<?>) operation.get("rules");
        var policy = loader.policy();
        assertThat(policy.operation().operationId()).isEqualTo("searchAcademicInstitutions");
        for (int i = 0; i < sourceRules.size(); i++) {
            Map<?, ?> source = (Map<?, ?>) sourceRules.get(i);
            assertThat(policy.operation().rules().get(i).limit()).isEqualTo(source.get("limit"));
            assertThat(policy.operation().rules().get(i).window()).isEqualTo(Duration.parse((String) source.get("window")));
            assertThat(policy.operation().rules().get(i).counterTtl()).isEqualTo(Duration.parse((String) source.get("counterTtl")));
        }
        assertThat(policy.operation().excludedQueryParameters()).containsExactlyInAnyOrder("query", "cursor", "limit");
        assertThat(policy.hmac().readVersions()).contains(policy.hmac().writeVersion());
        Map<?, ?> cleanup = (Map<?, ?>) ((Map<?, ?>) spec.get("storage")).get("cleanup");
        Map<?, ?> thresholds = (Map<?, ?>) cleanup.get("expiredRowsIncidentThresholds");
        assertThat(policy.cleanup().interval()).isEqualTo(Duration.parse((String) cleanup.get("interval")));
        assertThat(policy.cleanup().expiredRowIncidentThreshold()).isEqualTo(((Number) thresholds.get("rowCountGreaterThan")).longValue());
        assertThat(policy.cleanup().oldestExpiredIncidentThreshold())
                .isEqualTo(Duration.parse((String) thresholds.get("oldestExpiredAgeGreaterThan")));
    }

    @Test
    void rejectsAnyUnapprovedPolicyChangeIncludingScopeActivationAndCrypto() throws IOException {
        String canonical = Files.readString(Path.of("../docs/policies/public-endpoint-rate-limits-v1.yaml"));
        for (String altered : java.util.List.of(
                canonical.replace("propractix.io/v1", "propractix.io/v2"),
                canonical.replace("status: CLOSED", "status: DRAFT"),
                canonical.replace("approvedCommit:", "unapprovedCommit:"),
                canonical.replace("searchAcademicInstitutions", "otherOperation"),
                canonical.replace("method: GET", "method: POST"),
                canonical.replace("/api/v1/academic-institutions", "/api/v1/other"),
                canonical.replace("FIXED_WINDOW", "UNKNOWN"),
                canonical.replace("limit: 15", "limit: 16"),
                canonical.replace("PT70S", "PT1S"),
                canonical.replace("HMAC-SHA-256", "SHA-256"),
                canonical.replace("rl-ip-v1", "unapproved-version"),
                canonical.replace("FAIL_CLOSED", "FAIL_OPEN"),
                canonical.replace("    - operationId:", "    - operationId: other\n    - operationId:"),
                canonical + "\nsecret: DO-NOT-LEAK-THIS\n", "", "[malformed YAML")) {
            assertThatThrownBy(() -> loader.load(new ByteArrayInputStream(altered.getBytes(StandardCharsets.UTF_8))))
                    .isInstanceOf(RateLimitFailure.class).hasMessage("rate_limiter_unavailable:configuration").hasNoCause();
        }
    }

    @Test
    void missingUnreadableAndOversizedPoliciesHaveOnlyASafeDiagnostic() {
        assertThatThrownBy(() -> loader.load(null)).isInstanceOf(RateLimitFailure.class).hasNoCause();
        assertThatThrownBy(() -> loader.load(new ByteArrayInputStream(new byte[65537])))
                .isInstanceOf(RateLimitFailure.class).hasNoCause();
        assertThatThrownBy(() -> loader.load(new InputStream() {
            @Override public int read() throws IOException { throw new IOException("DO-NOT-LEAK-THIS"); }
        })).hasMessage("rate_limiter_unavailable:configuration").hasNoCause();
    }
}
