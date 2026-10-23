package com.propractix.platform.ratelimit.adapter.in.http;

import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** The filter and security share the same exact transport boundary, including encoded-path rejection. */
public final class RateLimitedHttpOperation implements RequestMatcher {
    private final RateLimitPolicy.Operation operation;

    public RateLimitedHttpOperation(RateLimitPolicy.Operation operation) {
        this.operation = operation;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return operation.method().equals(request.getMethod())
                && (request.getContextPath() + operation.pathTemplate()).equals(request.getRequestURI());
    }

    public String operationId() { return operation.operationId(); }
    public String path() { return operation.pathTemplate(); }
}
