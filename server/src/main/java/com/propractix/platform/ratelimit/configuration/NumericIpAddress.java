package com.propractix.platform.ratelimit.configuration;

import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import java.net.InetAddress;
import java.net.UnknownHostException;

/** Numeric literals only. A hostname can never reach InetAddress or trigger DNS. */
final class NumericIpAddress {
    private NumericIpAddress() { }

    static byte[] parse(String input) {
        if (input == null || input.isEmpty() || input.length() > 128) throw invalid();
        String address = input;
        int zone = address.indexOf('%');
        if (zone >= 0) {
            if (!address.contains(":") || !address.substring(zone + 1).matches("[A-Za-z0-9_.-]{1,64}")) {
                throw invalid();
            }
            address = address.substring(0, zone);
        }
        if (!address.contains(":")) return ipv4(address);
        if (!address.matches("[0-9a-fA-F:.]+")) throw invalid();
        if (address.contains(".")) {
            int lastColon = address.lastIndexOf(':');
            ipv4(address.substring(lastColon + 1));
        }
        try {
            return InetAddress.getByName(address).getAddress();
        } catch (UnknownHostException | IllegalArgumentException ex) {
            throw invalid();
        }
    }

    private static byte[] ipv4(String input) {
        String[] parts = input.split("\\.", -1);
        if (parts.length != 4) throw invalid();
        byte[] result = new byte[4];
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].matches("0|[1-9][0-9]{0,2}")) throw invalid();
            int value = Integer.parseInt(parts[i]);
            if (value > 255) throw invalid();
            result[i] = (byte) value;
        }
        return result;
    }

    private static RateLimitFailure invalid() {
        return new RateLimitFailure(RateLimitFailure.ErrorClass.CLIENT_IP);
    }
}
