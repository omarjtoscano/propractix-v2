package com.propractix.platform.ratelimit.configuration;

import com.propractix.platform.ratelimit.application.model.RateLimitFailure;

public final class TrustedProxyCidr {
    private final byte[] network;
    private final int prefix;

    public TrustedProxyCidr(String value) {
        try {
            if (value == null || value.contains("%")) throw new IllegalArgumentException();
            String[] parts = value.split("/", -1);
            if (parts.length != 2 || !parts[1].matches("[1-9][0-9]{0,2}")) {
                throw new IllegalArgumentException();
            }
            network = NumericIpAddress.parse(parts[0]);
            // IPv4-mapped literals have ambiguous CIDR prefixes: configure an IPv4 CIDR instead.
            if (network.length == 4 && parts[0].contains(":")) throw new IllegalArgumentException();
            prefix = Integer.parseInt(parts[1]);
            if (prefix > network.length * 8) throw new IllegalArgumentException();
            for (int bit = prefix; bit < network.length * 8; bit++) {
                if ((network[bit / 8] & (1 << (7 - bit % 8))) != 0) throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
        }
    }

    boolean contains(byte[] address) {
        if (address.length != network.length) return false;
        for (int bit = 0; bit < prefix; bit++) {
            int mask = 1 << (7 - bit % 8);
            if ((address[bit / 8] & mask) != (network[bit / 8] & mask)) return false;
        }
        return true;
    }

    @Override public String toString() { return "TrustedProxyCidr[redacted]"; }
}
