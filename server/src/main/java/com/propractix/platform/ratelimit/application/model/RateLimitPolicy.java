package com.propractix.platform.ratelimit.application.model;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/** Technical policy only; no catalog or tenant model is imported. */
public record RateLimitPolicy(
        String policyId, int version, Operation operation, Hmac hmac, Duration cleanupInterval) {
    public RateLimitPolicy {
        if (policyId == null || policyId.isBlank() || version < 1 || operation == null || hmac == null
                || cleanupInterval == null || cleanupInterval.isNegative() || cleanupInterval.isZero()) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
        }
    }

    public record Operation(String operationId, String method, String pathTemplate, String contractVersion,
                            Set<String> excludedQueryParameters, List<Rule> rules) {
        public Operation {
            excludedQueryParameters = Set.copyOf(excludedQueryParameters);
            rules = List.copyOf(rules);
            if (operationId == null || operationId.isBlank() || method == null || pathTemplate == null
                    || contractVersion == null || rules.isEmpty()
                    || rules.stream().map(Rule::ruleId).distinct().count() != rules.size()) {
                throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
            }
        }
    }

    public record Rule(String ruleId, int limit, Duration window, Duration counterTtl) {
        public Rule {
            if (ruleId == null || !ruleId.matches("[a-z][a-z0-9_]{0,63}") || limit < 1 || window == null
                    || counterTtl == null || window.isNegative() || window.isZero()
                    || window.getNano() != 0 || counterTtl.getNano() != 0 || counterTtl.compareTo(window) <= 0) {
                throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
            }
        }
    }

    public record Hmac(String algorithm, String purpose, String domainSeparator, String encoding,
                       String writeVersion, List<String> readVersions) {
        public Hmac {
            readVersions = List.copyOf(readVersions);
            if (!"HMAC-SHA-256".equals(algorithm) || !"RATE_LIMIT_CLIENT_IP".equals(purpose)
                    || domainSeparator == null || domainSeparator.isBlank()
                    || !"BASE64URL_NO_PADDING".equals(encoding) || readVersions.isEmpty()
                    || readVersions.stream().anyMatch(v -> !v.matches("[a-z][a-z0-9-]{0,63}"))
                    || readVersions.stream().distinct().count() != readVersions.size()
                    || !readVersions.contains(writeVersion)) {
                throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
            }
        }

        @Override public String toString() { return "Hmac[redacted]"; }
    }
}
