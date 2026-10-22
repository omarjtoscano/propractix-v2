package com.propractix.platform.ratelimit.application.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RateLimitModelTest {
    @Test
    void rejectsNonPositiveOrFractionalWindowsAndTtlNotExceedingWindow() {
        for (Duration window : List.of(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofMillis(1))) {
            assertThatThrownBy(() -> new RateLimitPolicy.Rule("test", 1, window, Duration.ofSeconds(20)))
                    .isInstanceOf(RateLimitFailure.class);
        }
        assertThatThrownBy(() -> new RateLimitPolicy.Rule("test", 0, Duration.ofSeconds(1), Duration.ofSeconds(2)))
                .isInstanceOf(RateLimitFailure.class);
        assertThatThrownBy(() -> new RateLimitPolicy.Rule("test", 1, Duration.ofSeconds(2), Duration.ofSeconds(2)))
                .isInstanceOf(RateLimitFailure.class);
    }

    @Test
    void rejectsInconsistentHmacVersionsAlgorithmsAndPurpose() {
        assertThatThrownBy(() -> hmac("HMAC-SHA-256", "RATE_LIMIT_CLIENT_IP", "new", List.of("old")))
                .isInstanceOf(RateLimitFailure.class);
        assertThatThrownBy(() -> hmac("HMAC-SHA-256", "RATE_LIMIT_CLIENT_IP", "old", List.of("old", "old")))
                .isInstanceOf(RateLimitFailure.class);
        assertThatThrownBy(() -> hmac("HMAC-SHA-1", "RATE_LIMIT_CLIENT_IP", "old", List.of("old")))
                .isInstanceOf(RateLimitFailure.class);
        assertThatThrownBy(() -> hmac("HMAC-SHA-256", "TRACKING", "old", List.of("old")))
                .isInstanceOf(RateLimitFailure.class);
    }

    @Test
    void networkSubjectDefensivelyCopiesInputAndOutputAndDoesNotLogIt() {
        byte[] address = {(byte) 192, 0, 2, 9};
        var subject = new ClientNetworkSubject(address);
        byte[] expected = subject.canonicalBytes();
        java.util.Arrays.fill(address, (byte) 0);
        byte[] returned = subject.canonicalBytes();
        java.util.Arrays.fill(returned, (byte) 0);
        assertThat(subject.canonicalBytes()).isEqualTo(expected);
        assertThat(subject.toString()).isEqualTo("ClientNetworkSubject[redacted]");
    }

    @Test
    void admissionDecisionCannotRepresentPermissiveOrPartialFailure() {
        assertThatThrownBy(() -> new RateLimitDecision(true, Duration.ofSeconds(1), Set.of()))
                .isInstanceOf(RateLimitFailure.class);
        assertThatThrownBy(() -> new RateLimitDecision(true, Duration.ZERO, Set.of("test")))
                .isInstanceOf(RateLimitFailure.class);
        assertThatThrownBy(() -> new RateLimitDecision(false, Duration.ZERO, Set.of("test")))
                .isInstanceOf(RateLimitFailure.class);
        assertThatThrownBy(() -> new RateLimitDecision(false, Duration.ofSeconds(1), Set.of()))
                .isInstanceOf(RateLimitFailure.class);
    }

    private static RateLimitPolicy.Hmac hmac(String algorithm, String purpose, String write, List<String> reads) {
        return new RateLimitPolicy.Hmac(algorithm, purpose, "test-domain\n", "BASE64URL_NO_PADDING", write, reads);
    }
}
