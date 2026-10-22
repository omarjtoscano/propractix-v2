package com.propractix.platform.ratelimit.application.port.out;

import com.propractix.platform.ratelimit.application.model.ClientIpFingerprints;
import com.propractix.platform.ratelimit.application.model.RateLimitDecision;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;

/** Claims every rule and active HMAC version atomically, using the database clock. */
public interface RateLimitBucketStore {
    RateLimitDecision claim(RateLimitPolicy policy, ClientIpFingerprints fingerprints);
}
