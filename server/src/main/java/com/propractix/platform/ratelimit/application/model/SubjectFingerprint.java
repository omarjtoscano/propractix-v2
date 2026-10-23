package com.propractix.platform.ratelimit.application.model;

public final class SubjectFingerprint {
    private final String value;

    public SubjectFingerprint(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{43}")) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.KEY);
        }
        this.value = value;
    }

    /** Sensitive pseudonym: only the bucket store may persist this value. */
    public String value() { return value; }
    @Override public String toString() { return "SubjectFingerprint[redacted]"; }
}
