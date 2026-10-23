package com.propractix.platform.ratelimit.application.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

/** Short-lived canonical network, never a loggable identifier. */
public final class ClientNetworkSubject {
    private final byte[] network;

    public ClientNetworkSubject(byte[] address) {
        if (address == null || (address.length != 4 && address.length != 16)) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.CLIENT_IP);
        }
        network = address.clone();
        if (network.length == 16) Arrays.fill(network, 8, 16, (byte) 0);
    }

    /** Family, fixed-width lowercase network hex, and prefix; UTF-8 with no trailing newline. */
    public byte[] canonicalBytes() {
        return ((network.length == 4 ? "ipv4:" : "ipv6:") + HexFormat.of().formatHex(network)
                + (network.length == 4 ? "/32" : "/64")).getBytes(StandardCharsets.UTF_8);
    }

    @Override public String toString() { return "ClientNetworkSubject[redacted]"; }
}
