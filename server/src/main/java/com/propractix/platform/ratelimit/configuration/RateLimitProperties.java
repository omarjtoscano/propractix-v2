package com.propractix.platform.ratelimit.configuration;

import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import java.util.List;

/** Bound explicitly inside a sanitized boundary; no secret material is bound into this object. */
public record RateLimitProperties(IpMode ipMode, List<String> trustedProxyCidrs,
                                  boolean caddySanitizationConfiguredAndTested) {
    public enum IpMode { REMOTE_ADDRESS, X_FORWARDED_FOR }

    public RateLimitProperties {
        trustedProxyCidrs = trustedProxyCidrs == null ? List.of() : List.copyOf(trustedProxyCidrs);
        if (ipMode == null
                || (ipMode == IpMode.REMOTE_ADDRESS
                    && (!trustedProxyCidrs.isEmpty() || caddySanitizationConfiguredAndTested))
                || (ipMode == IpMode.X_FORWARDED_FOR
                    && (trustedProxyCidrs.isEmpty() || !caddySanitizationConfiguredAndTested))) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
        }
        trustedProxyCidrs.forEach(TrustedProxyCidr::new);
    }

    @Override public String toString() { return "RateLimitProperties[redacted]"; }
}
