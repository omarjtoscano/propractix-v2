package com.propractix.platform.ratelimit.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import com.propractix.platform.ratelimit.configuration.CanonicalRateLimitPolicyLoader;
import com.propractix.platform.ratelimit.configuration.ConfiguredClientIpResolver;
import com.propractix.platform.ratelimit.configuration.RateLimitProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HmacClientIpFingerprintTest {
    private final RateLimitPolicy.Hmac canonical = new CanonicalRateLimitPolicyLoader().policy().hmac();
    private final byte[] key = "SYNTHETIC-LOCAL-ONLY-RATE-LIMIT-HMAC-NOT-FOR-DEPLOYMENT-v1".getBytes(StandardCharsets.UTF_8);
    private final ConfiguredClientIpResolver resolver = new ConfiguredClientIpResolver(
            new RateLimitProperties(RateLimitProperties.IpMode.REMOTE_ADDRESS, List.of(), false));

    @Test
    void matchesAnIndependentHmacSha256VectorAndHasUrlSafeUnpaddedEncoding() {
        var adapter = new HmacClientIpFingerprint(canonical, Map.of(canonical.writeVersion(), key));
        String fingerprint = adapter.fingerprint(subject("192.0.2.9")).get(canonical.writeVersion()).value();
        // Independently calculated with Python's hmac/hashlib for the documented canonical input.
        assertThat(fingerprint).isEqualTo("e5uRm1jkAZWGEoJsketq12NqZDBGWtzPWl6Lc-4L-6s");
        assertThat(fingerprint).matches("[A-Za-z0-9_-]{43}").doesNotContain("=", "+", "/");
    }

    @Test
    void isDeterministicAndSharesIpv6QuotaOnlyWithinTheSame64Network() {
        var adapter = new HmacClientIpFingerprint(canonical, Map.of(canonical.writeVersion(), key));
        assertThat(value(adapter, "192.0.2.9")).isEqualTo(value(adapter, "::ffff:192.0.2.9"));
        assertThat(value(adapter, "192.0.2.9")).isNotEqualTo(value(adapter, "192.0.2.10"));
        assertThat(value(adapter, "2001:db8:1234:5678::1")).isEqualTo(value(adapter, "2001:db8:1234:5678::ffff%eth0"));
        assertThat(value(adapter, "2001:db8:1234:5678::1")).isNotEqualTo(value(adapter, "2001:db8:1234:5679::1"));
    }

    @Test
    void computesAllReadVersionsAndCopiesCallerOwnedKeyMaterial() {
        var policy = new RateLimitPolicy.Hmac(canonical.algorithm(), canonical.purpose(), canonical.domainSeparator(),
                canonical.encoding(), "new", List.of("old", "new"));
        byte[] oldKey = key.clone();
        byte[] newKey = "SYNTHETIC-ROTATION-ONLY-NOT-A-REAL-KEY-v2".getBytes(StandardCharsets.UTF_8);
        var adapter = new HmacClientIpFingerprint(policy, Map.of("old", oldKey, "new", newKey));
        java.util.Arrays.fill(oldKey, (byte) 0);
        var fingerprints = adapter.fingerprint(subject("192.0.2.9"));
        assertThat(fingerprints.versions()).containsExactlyInAnyOrder("old", "new");
        assertThat(fingerprints.get("old").value()).isEqualTo("e5uRm1jkAZWGEoJsketq12NqZDBGWtzPWl6Lc-4L-6s");
        assertThat(fingerprints.get("new").value()).isNotEqualTo(fingerprints.get("old").value());
    }

    @Test
    void domainSeparatorChangesTheFingerprint() {
        var policy = new RateLimitPolicy.Hmac(canonical.algorithm(), canonical.purpose(), "other-purpose\n",
                canonical.encoding(), canonical.writeVersion(), canonical.readVersions());
        var other = new HmacClientIpFingerprint(policy, Map.of(canonical.writeVersion(), key));
        var standard = new HmacClientIpFingerprint(canonical, Map.of(canonical.writeVersion(), key));
        assertThat(value(other, "192.0.2.9")).isNotEqualTo(value(standard, "192.0.2.9"));
    }

    @Test
    void missingEmptyOrShortActiveKeysFailWithoutCausesOrPartialResults() {
        for (Map<String, byte[]> material : List.of(Map.<String, byte[]>of(), Map.of(canonical.writeVersion(), new byte[0]),
                Map.of(canonical.writeVersion(), "SHORT-SECRET-DO-NOT-LEAK".getBytes(StandardCharsets.UTF_8)))) {
            assertThatThrownBy(() -> new HmacClientIpFingerprint(canonical, material))
                    .isInstanceOf(RateLimitFailure.class).hasMessage("rate_limiter_unavailable:key").hasNoCause();
        }
        var policy = new RateLimitPolicy.Hmac(canonical.algorithm(), canonical.purpose(), canonical.domainSeparator(),
                canonical.encoding(), "new", List.of("old", "new"));
        assertThatThrownBy(() -> new HmacClientIpFingerprint(policy, Map.of("new", key)))
                .isInstanceOf(RateLimitFailure.class).hasNoCause();
        var adapter = new HmacClientIpFingerprint(canonical, Map.of(canonical.writeVersion(), key));
        assertThatThrownBy(() -> adapter.fingerprint(null)).hasMessage("rate_limiter_unavailable:key").hasNoCause();
    }

    @Test
    void diagnosticRepresentationsRedactNetworksFingerprintsKeysAndVersions() {
        var adapter = new HmacClientIpFingerprint(canonical, Map.of(canonical.writeVersion(), key));
        var subject = subject("192.0.2.9");
        var fingerprint = adapter.fingerprint(subject).get(canonical.writeVersion());
        assertThat(subject.toString() + fingerprint + adapter + canonical + adapter.fingerprint(subject))
                .doesNotContain("192.0.2.9", "c0000209", fingerprint.value(), canonical.writeVersion(),
                        new String(key, StandardCharsets.UTF_8));
    }

    private ClientNetworkSubject subject(String address) { return resolver.resolve(address, null, null); }
    private String value(HmacClientIpFingerprint adapter, String address) {
        return adapter.fingerprint(subject(address)).get(canonical.writeVersion()).value();
    }
}
