package com.propractix.platform.ratelimit.configuration;

import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.port.out.ClientIpResolver;
import java.util.Arrays;
import java.util.List;

public final class ConfiguredClientIpResolver implements ClientIpResolver {
    private final RateLimitProperties.IpMode mode;
    private final List<TrustedProxyCidr> proxies;

    public ConfiguredClientIpResolver(RateLimitProperties properties) {
        mode = properties.ipMode();
        proxies = properties.trustedProxyCidrs().stream().map(TrustedProxyCidr::new).toList();
    }

    @Override
    public ClientNetworkSubject resolve(String remoteAddress, List<String> xForwardedFor, List<String> forwarded) {
        byte[] peer = NumericIpAddress.parse(remoteAddress);
        if (mode == RateLimitProperties.IpMode.REMOTE_ADDRESS || !isTrusted(peer)) {
            return new ClientNetworkSubject(peer);
        }
        if (xForwardedFor == null || xForwardedFor.size() != 1 || xForwardedFor.getFirst() == null
                || xForwardedFor.getFirst().length() > 8192 || (forwarded != null && !forwarded.isEmpty())) {
            throw invalid();
        }
        String[] entries = xForwardedFor.getFirst().split(",", -1);
        if (entries.length > 64) throw invalid();
        // Validate the entire chain, including values left of the first untrusted hop.
        List<byte[]> hops = Arrays.stream(entries).map(entry -> entry.replaceAll("^[ \t]+|[ \t]+$", "")).map(NumericIpAddress::parse).toList();
        for (int i = hops.size() - 1; i >= 0; i--) {
            if (!isTrusted(hops.get(i))) return new ClientNetworkSubject(hops.get(i));
        }
        throw invalid();
    }

    private boolean isTrusted(byte[] address) {
        return proxies.stream().anyMatch(cidr -> cidr.contains(address));
    }

    private static RateLimitFailure invalid() {
        return new RateLimitFailure(RateLimitFailure.ErrorClass.CLIENT_IP);
    }
}
