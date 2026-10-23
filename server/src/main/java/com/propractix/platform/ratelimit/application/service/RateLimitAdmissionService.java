package com.propractix.platform.ratelimit.application.service;

import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitDecision;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.port.in.RateLimitAdmission;
import com.propractix.platform.ratelimit.application.port.out.ClientIpFingerprint;
import com.propractix.platform.ratelimit.application.port.out.RateLimitBucketStore;
import com.propractix.platform.ratelimit.application.port.out.RateLimitPolicyProvider;

public final class RateLimitAdmissionService implements RateLimitAdmission {
    private final RateLimitPolicyProvider provider;
    private final ClientIpFingerprint fingerprint;
    private final RateLimitBucketStore store;

    public RateLimitAdmissionService(RateLimitPolicyProvider provider, ClientIpFingerprint fingerprint,
                                     RateLimitBucketStore store) {
        this.provider = provider;
        this.fingerprint = fingerprint;
        this.store = store;
    }

    @Override
    public RateLimitDecision admit(String operationId, ClientNetworkSubject subject) {
        var policy = provider.policy();
        if (!policy.operation().operationId().equals(operationId)) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
        }
        if (subject == null) throw new RateLimitFailure(RateLimitFailure.ErrorClass.CLIENT_IP);
        return store.claim(policy, fingerprint.fingerprint(subject));
    }
}
