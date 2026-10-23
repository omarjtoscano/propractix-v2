package com.propractix.platform.ratelimit.adapter.in.http;

import com.propractix.platform.observability.CorrelationIdFilter;
import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitDecision;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.port.in.RateLimitAdmission;
import com.propractix.platform.ratelimit.application.port.out.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/** Registered only as a servlet filter, after correlation and before Spring Security. */
public final class HttpRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitedHttpOperation operation;
    private final ClientIpResolver resolver;
    private final RateLimitAdmission admission;
    private final JsonMapper json;
    private final Consumer<RateLimitFailure.ErrorClass> errors;

    public HttpRateLimitFilter(RateLimitedHttpOperation operation, ClientIpResolver resolver,
                               RateLimitAdmission admission, JsonMapper json,
                               Consumer<RateLimitFailure.ErrorClass> errors) {
        this.operation = operation;
        this.resolver = resolver;
        this.admission = admission;
        this.json = json;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !operation.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ClientNetworkSubject subject;
        try {
            subject = resolver.resolve(request.getRemoteAddr(), Collections.list(request.getHeaders("X-Forwarded-For")),
                    Collections.list(request.getHeaders("Forwarded")));
        } catch (RuntimeException failure) {
            errors.accept(RateLimitFailure.ErrorClass.CLIENT_IP);
            problem(response, 503, "rate_limiter_unavailable", 1);
            return;
        }

        RateLimitDecision decision;
        try {
            decision = admission.admit(operation.operationId(), subject);
            if (decision == null) throw new IllegalStateException();
        } catch (RuntimeException failure) {
            // Admission meters its typed failures. No provider message, cause or request is logged or serialized.
            if (!(failure instanceof RateLimitFailure)) errors.accept(RateLimitFailure.ErrorClass.STORAGE);
            problem(response, 503, "rate_limiter_unavailable", 1);
            return;
        }
        if (!decision.allowed()) {
            problem(response, 429, "rate_limited", decision.retryAfter().toSeconds());
            return;
        }
        // Downstream failures belong to security/MVC, never to the rate limiter's failure boundary.
        chain.doFilter(request, response);
    }

    private void problem(HttpServletResponse response, int status, String code, long retryAfter) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create("urn:propractix:problem:" + code));
        problem.setTitle(code);
        problem.setDetail(code);
        problem.setInstance(URI.create(operation.path()));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        problem.setProperty("violations", List.of());
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", Long.toString(retryAfter));
        json.writeValue(response.getOutputStream(), problem);
    }
}
