package com.propractix.platform.ratelimit.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientIpResolverTest {
    private final ConfiguredClientIpResolver direct = new ConfiguredClientIpResolver(
            new RateLimitProperties(RateLimitProperties.IpMode.REMOTE_ADDRESS, List.of(), false));
    private final ConfiguredClientIpResolver proxy = new ConfiguredClientIpResolver(new RateLimitProperties(
            RateLimitProperties.IpMode.X_FORWARDED_FOR, List.of("10.0.0.0/8", "2001:db8:ffff::/48"), true));

    @Test
    void directAndUntrustedPeersIgnoreBothHeaderFamiliesEvenIfMalformed() {
        assertThat(direct.resolve("192.0.2.9", List.of("invalid\r\nlog"), List.of("for=attacker")).canonicalBytes())
                .isEqualTo("ipv4:c0000209/32".getBytes(StandardCharsets.UTF_8));
        assertThat(proxy.resolve("192.0.2.9", List.of("10.0.0.1"), List.of("for=attacker")).canonicalBytes())
                .isEqualTo(direct.resolve("192.0.2.9", null, null).canonicalBytes());
    }

    @Test
    void traversesFromRightToLeftAndStopsAtFirstUntrustedHop() {
        assertThat(proxy.resolve("10.0.0.1", List.of("198.51.100.7, 192.0.2.9, 10.2.3.4"), List.of())
                .canonicalBytes()).isEqualTo(direct.resolve("192.0.2.9", null, null).canonicalBytes());
        assertThat(proxy.resolve("2001:db8:ffff::1", List.of("192.0.2.9, 10.2.3.4"), List.of())
                .canonicalBytes()).isEqualTo(direct.resolve("192.0.2.9", null, null).canonicalBytes());
    }

    @Test
    void mappedIpv4NormalizesAsIpv4IncludingHexNotation() {
        byte[] expected = direct.resolve("192.0.2.9", null, null).canonicalBytes();
        assertThat(direct.resolve("::ffff:192.0.2.9", null, null).canonicalBytes()).isEqualTo(expected);
        assertThat(direct.resolve("0:0:0:0:0:ffff:c000:209", null, null).canonicalBytes()).isEqualTo(expected);
        assertThat(proxy.resolve("::ffff:10.0.0.1", List.of("192.0.2.9"), null).canonicalBytes()).isEqualTo(expected);
    }

    @Test
    void ipv6CanonicalizesZonesCompressionAndHostBitsButSeparatesNetworks() {
        byte[] expected = direct.resolve("2001:db8:1234:5678::1", null, null).canonicalBytes();
        assertThat(direct.resolve("2001:0db8:1234:5678:abcd:abcd:abcd:abcd%eth0", null, null).canonicalBytes())
                .isEqualTo(expected);
        assertThat(direct.resolve("2001:db8:1234:5679::1", null, null).canonicalBytes()).isNotEqualTo(expected);
        assertThat(new String(expected, StandardCharsets.UTF_8)).isEqualTo("ipv6:20010db8123456780000000000000000/64");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "example.invalid", "127.1", "2130706433", "0300.0.2.9", "192.00.2.9",
            "256.0.2.9", "-1.0.2.9", "+1.0.2.9", "192.0.2.9:80", " 192.0.2.9", "192.0.2.9%eth0",
            "[2001:db8::1]", "2001:db8::1%", "2001:db8::1%a%b", "::ffff:192.00.2.9", "2001:::1", "::1\r\n"})
    void rejectsAmbiguousAddressesAndHostnamesWithoutLeakingInput(String address) {
        assertThatThrownBy(() -> direct.resolve(address, null, null))
                .isInstanceOf(RateLimitFailure.class).hasMessage("rate_limiter_unavailable:client_ip").hasNoCause();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown", "192.0.2.9,", "invalid, 192.0.2.9", "10.1.1.1, 10.2.2.2",
            "192.0.2.9\r\n", "192.0.2.9:443", "\"192.0.2.9\"", "192.0.2.9\r\nInjected: value"})
    void malformedOrUnresolvableTrustedChainsFailClosed(String header) {
        assertThatThrownBy(() -> proxy.resolve("10.0.0.1", List.of(header), List.of()))
                .isInstanceOf(RateLimitFailure.class).hasNoCause();
    }

    @Test
    void rejectsMissingDuplicateConflictingAndOversizedTrustedHeaders() {
        for (List<String> headers : List.of(List.<String>of(), List.of("192.0.2.9", "198.51.100.1"),
                List.of("1".repeat(8193)), List.of("192.0.2.9,".repeat(65)))) {
            assertThatThrownBy(() -> proxy.resolve("10.0.0.1", headers, null)).isInstanceOf(RateLimitFailure.class);
        }
        assertThatThrownBy(() -> proxy.resolve("10.0.0.1", null, null)).isInstanceOf(RateLimitFailure.class);
        assertThatThrownBy(() -> proxy.resolve("10.0.0.1", List.of("192.0.2.9"), List.of("for=192.0.2.9")))
                .isInstanceOf(RateLimitFailure.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0/0", "::/0", "10.0.0.0/33", "2001:db8::/129", "10.0.0.1/8",
            "10.0.0.0/08", "10.0.0.0", "example.invalid/24", "::ffff:10.0.0.0/8", "fe80::%eth0/64"})
    void rejectsUnsafeProxyCidrs(String cidr) {
        assertThatThrownBy(() -> new TrustedProxyCidr(cidr))
                .isInstanceOf(RateLimitFailure.class).hasMessage("rate_limiter_unavailable:configuration").hasNoCause();
    }

    @Test
    void matchesNonByteAlignedPrefixesWithoutTrustingNeighbors() {
        var cidr = new TrustedProxyCidr("192.0.2.0/25");
        assertThat(cidr.contains(NumericIpAddress.parse("192.0.2.127"))).isTrue();
        assertThat(cidr.contains(NumericIpAddress.parse("192.0.2.128"))).isFalse();
        assertThat(cidr.contains(NumericIpAddress.parse("2001:db8::1"))).isFalse();
    }
}
