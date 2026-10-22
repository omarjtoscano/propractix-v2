package com.propractix.platform.ratelimit.application.model;

import java.time.Duration;
import java.util.Set;

public record RateLimitDecision(boolean allowed, Duration retryAfter, Set<String> violatedRuleIds) {
    public RateLimitDecision {
        violatedRuleIds = Set.copyOf(violatedRuleIds);
        if (retryAfter == null || retryAfter.getNano() != 0
                || (allowed && (!retryAfter.isZero() || !violatedRuleIds.isEmpty()))
                || (!allowed && (retryAfter.toSeconds() < 1 || violatedRuleIds.isEmpty()))) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.STORAGE);
        }
    }
}
