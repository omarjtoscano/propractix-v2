package com.propractix.platform.ratelimit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.platform.ratelimit.adapter.out.observability.MeteredRateLimitAdmission;
import com.propractix.platform.ratelimit.application.model.ClientIpFingerprints;
import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.model.SubjectFingerprint;
import com.propractix.platform.ratelimit.configuration.CanonicalRateLimitPolicyLoader;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RateLimitAdmissionServiceTest {
    private final CanonicalRateLimitPolicyLoader provider = new CanonicalRateLimitPolicyLoader();
    private final ClientNetworkSubject subject = new ClientNetworkSubject(new byte[]{(byte) 192, 0, 2, 9});

    @Test
    void unlistedOperationOrMissingSubjectCannotComputeFingerprintsOrReachStore() {
        AtomicInteger calls = new AtomicInteger();
        var service = new RateLimitAdmissionService(provider, input -> {
            calls.incrementAndGet();
            throw new AssertionError("Must not fingerprint rejected input");
        }, (policy, fingerprints) -> { throw new AssertionError("Must not call storage"); });
        assertThatThrownBy(() -> service.admit("unlisted", subject)).hasMessage("rate_limiter_unavailable:configuration");
        assertThatThrownBy(() -> service.admit(provider.policy().operation().operationId(), null))
                .hasMessage("rate_limiter_unavailable:client_ip");
        assertThat(calls).hasValue(0);
    }

    @Test
    void keyFailureCannotConsumeQuotaAndHasBoundedErrorMetric() {
        var registry = new SimpleMeterRegistry();
        var service = new RateLimitAdmissionService(provider, input -> {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.KEY);
        }, (policy, fingerprints) -> { throw new AssertionError("Must not call storage"); });
        var measured = new MeteredRateLimitAdmission(service, provider.policy(), registry);
        assertThatThrownBy(() -> measured.admit(provider.policy().operation().operationId(), subject))
                .hasMessage("rate_limiter_unavailable:key").hasNoCause();
        assertThat(registry.get("propractix_rate_limit_adapter_errors_total").tag("error_class", "key").counter().count()).isEqualTo(1);
        assertThat(registry.get("propractix_rate_limit_allowed_total").counter().count()).isZero();
    }

    @Test
    void storeReceivesCompleteFingerprintSetAndStorageFailureCannotBecomeAllowed() {
        var fingerprints = new ClientIpFingerprints(Map.of(provider.policy().hmac().writeVersion(),
                new SubjectFingerprint("a".repeat(43))));
        var registry = new SimpleMeterRegistry();
        var service = new RateLimitAdmissionService(provider, input -> fingerprints, (policy, received) -> {
            assertThat(policy).isSameAs(provider.policy());
            assertThat(received).isSameAs(fingerprints);
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.STORAGE);
        });
        var measured = new MeteredRateLimitAdmission(service, provider.policy(), registry);
        assertThatThrownBy(() -> measured.admit(provider.policy().operation().operationId(), subject))
                .hasMessage("rate_limiter_unavailable:storage").hasNoCause();
        assertThat(registry.get("propractix_rate_limit_adapter_errors_total").tag("error_class", "storage").counter().count()).isEqualTo(1);
        assertThat(registry.get("propractix_rate_limit_allowed_total").counter().count()).isZero();
    }
}
