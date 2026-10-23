package com.propractix.platform.ratelimit.application.model;

import java.time.Duration;

/** Aggregate maintenance evidence, with no subject or key identifiers. */
public record RateLimitBacklog(long expiredRows, Duration oldestExpiredAge) {
    public boolean incident(RateLimitPolicy.Cleanup policy) {
        return expiredRows > policy.expiredRowIncidentThreshold()
                || oldestExpiredAge.compareTo(policy.oldestExpiredIncidentThreshold()) > 0;
    }
}
