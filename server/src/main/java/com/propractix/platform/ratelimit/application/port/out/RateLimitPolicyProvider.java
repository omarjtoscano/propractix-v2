package com.propractix.platform.ratelimit.application.port.out;

import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;

public interface RateLimitPolicyProvider {
    RateLimitPolicy policy();
}
