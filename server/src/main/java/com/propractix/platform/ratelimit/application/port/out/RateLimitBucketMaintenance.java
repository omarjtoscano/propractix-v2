package com.propractix.platform.ratelimit.application.port.out;

import com.propractix.platform.ratelimit.application.model.RateLimitBacklog;

public interface RateLimitBucketMaintenance {
    RateLimitBacklog backlog();
    int deleteExpiredBatch(int batchSize);
    void verifyAvailable();
}
