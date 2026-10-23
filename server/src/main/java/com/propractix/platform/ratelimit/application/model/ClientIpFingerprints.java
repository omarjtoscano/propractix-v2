package com.propractix.platform.ratelimit.application.model;

import java.util.Map;
import java.util.Set;

/** Redacts both pseudonyms and key-version identifiers in diagnostic output. */
public final class ClientIpFingerprints {
    private final Map<String, SubjectFingerprint> values;

    public ClientIpFingerprints(Map<String, SubjectFingerprint> values) {
        if (values == null || values.isEmpty()) throw new RateLimitFailure(RateLimitFailure.ErrorClass.KEY);
        this.values = Map.copyOf(values);
    }

    public Set<String> versions() { return values.keySet(); }
    public SubjectFingerprint get(String version) {
        SubjectFingerprint value = values.get(version);
        if (value == null) throw new RateLimitFailure(RateLimitFailure.ErrorClass.KEY);
        return value;
    }
    @Override public String toString() { return "ClientIpFingerprints[redacted]"; }
}
