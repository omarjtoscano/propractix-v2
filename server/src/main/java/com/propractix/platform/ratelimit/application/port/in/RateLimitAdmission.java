package com.propractix.platform.ratelimit.application.port.in;

import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitDecision;

/** Unavailable admission must never imply allowed. */
public interface RateLimitAdmission {
    RateLimitDecision admit(String operationId, ClientNetworkSubject subject);
}
