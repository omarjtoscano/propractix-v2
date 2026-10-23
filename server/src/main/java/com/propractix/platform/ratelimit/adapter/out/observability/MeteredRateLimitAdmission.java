package com.propractix.platform.ratelimit.adapter.out.observability;

import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitDecision;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import com.propractix.platform.ratelimit.application.port.in.RateLimitAdmission;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Pre-registers the complete bounded label space from the approved policy and error taxonomy. */
public final class MeteredRateLimitAdmission implements RateLimitAdmission {
    private final RateLimitAdmission delegate;
    private final Counter allowed;
    private final Map<String, Counter> rejected = new HashMap<>();
    private final Map<RateLimitFailure.ErrorClass, Counter> errors = new EnumMap<>(RateLimitFailure.ErrorClass.class);

    public MeteredRateLimitAdmission(RateLimitAdmission delegate, RateLimitPolicy policy, MeterRegistry registry) {
        this.delegate = delegate;
        String operation = policy.operation().operationId();
        String version = Integer.toString(policy.version());
        allowed = registry.counter("propractix_rate_limit_allowed_total", "operation_id", operation, "policy_version", version);
        for (var rule : policy.operation().rules()) {
            rejected.put(rule.ruleId(), registry.counter("propractix_rate_limit_rejected_total",
                    "operation_id", operation, "policy_version", version, "rule_id", rule.ruleId()));
        }
        for (var error : RateLimitFailure.ErrorClass.values()) {
            errors.put(error, registry.counter("propractix_rate_limit_adapter_errors_total",
                    "operation_id", operation, "policy_version", version,
                    "error_class", error.name().toLowerCase(Locale.ROOT)));
        }
    }

    @Override
    public RateLimitDecision admit(String operationId, ClientNetworkSubject subject) {
        try {
            var decision = delegate.admit(operationId, subject);
            if (decision.allowed()) allowed.increment();
            else decision.violatedRuleIds().forEach(rule -> rejected.get(rule).increment());
            return decision;
        } catch (RateLimitFailure failure) {
            error(failure.errorClass());
            throw failure;
        }
    }

    public void error(RateLimitFailure.ErrorClass errorClass) { errors.get(errorClass).increment(); }
}
