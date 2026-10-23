package com.propractix.platform.ratelimit.application.model;

/** Safe diagnostic boundary: never attach an input, provider message or cause. */
public final class RateLimitFailure extends RuntimeException {
    public enum ErrorClass { CONFIGURATION, KEY, CLIENT_IP, STORAGE }

    private final ErrorClass errorClass;

    public RateLimitFailure(ErrorClass errorClass) {
        super("rate_limiter_unavailable:" + errorClass.name().toLowerCase(java.util.Locale.ROOT));
        this.errorClass = errorClass;
    }

    public ErrorClass errorClass() { return errorClass; }
}
