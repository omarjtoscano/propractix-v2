package com.propractix.platform.ratelimit.adapter.in.maintenance;

import com.propractix.platform.ratelimit.application.port.out.RateLimitBucketMaintenance;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Bounded transactions and SKIP LOCKED keep maintenance out of the admission path. */
public final class RateLimitCleanupJob {
    private static final Logger LOG = LoggerFactory.getLogger(RateLimitCleanupJob.class);
    private final RateLimitBucketMaintenance store;
    private final RateLimitPolicy policy;
    private final Consumer<RateLimitFailure.ErrorClass> errors;

    public RateLimitCleanupJob(RateLimitBucketMaintenance store, RateLimitPolicy policy, Consumer<RateLimitFailure.ErrorClass> errors) {
        this.store = store;
        this.policy = policy;
        this.errors = errors;
    }

    @Scheduled(fixedDelayString = "#{@rateLimitPolicyProvider.policy().cleanup().interval().toMillis()}",
               initialDelayString = "#{@rateLimitPolicyProvider.policy().cleanup().interval().toMillis()}")
    public void clean() {
        long started = System.nanoTime();
        try {
            var before = store.backlog();
            if (before.incident(policy.cleanup())) {
                // This stable alert signal requires Operations to open the incident described in the runbook.
                LOG.warn("rate_limit_cleanup_incident operation_id={} policy_version={} expired_rows={} oldest_expired_seconds={}",
                        policy.operation().operationId(), policy.version(), before.expiredRows(), before.oldestExpiredAge().toSeconds());
            }
            int deleted = 0;
            for (int batch = 0; batch < 100; batch++) {
                int count = store.deleteExpiredBatch(1000);
                deleted += count;
                if (count < 1000) break;
            }
            LOG.info("rate_limit_cleanup_completed operation_id={} policy_version={} deleted_rows={} elapsed_ms={}",
                    policy.operation().operationId(), policy.version(), deleted, (System.nanoTime() - started) / 1_000_000);
        } catch (RateLimitFailure failure) {
            errors.accept(failure.errorClass());
            LOG.warn("rate_limit_cleanup_failed operation_id={} policy_version={} error_class=storage",
                    policy.operation().operationId(), policy.version());
        }
    }
}
