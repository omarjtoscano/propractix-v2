package com.propractix.platform.ratelimit.adapter.out.observability;

import com.propractix.platform.ratelimit.application.port.out.RateLimitBucketMaintenance;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public final class RateLimitHealthIndicator implements HealthIndicator {
    private final RateLimitBucketMaintenance store;
    private final RateLimitPolicy policy;

    public RateLimitHealthIndicator(RateLimitBucketMaintenance store, RateLimitPolicy policy) {
        this.store = store;
        this.policy = policy;
    }

    @Override
    public Health health() {
        try {
            store.verifyAvailable();
            return store.backlog().incident(policy.cleanup()) ? Health.down().build() : Health.up().build();
        } catch (RuntimeException failure) {
            // Never use down(exception): JDBC causes may expose SQL, pseudonyms or credentials.
            return Health.down().build();
        }
    }
}
