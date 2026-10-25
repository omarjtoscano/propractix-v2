package com.propractix.platform.web;

import java.net.URI;
import java.util.List;
import org.springframework.http.ProblemDetail;

/** Builds only allowlisted codes and field names; callers never pass provider diagnostics. */
public final class PublicProblemDetails {
    private PublicProblemDetails() {}

    public static ProblemDetail create(int status, String code, String path, String correlationId,
                                       List<Violation> violations) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create("urn:propractix:problem:" + code));
        problem.setTitle(code);
        problem.setDetail(code);
        problem.setInstance(URI.create(path));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", correlationId);
        problem.setProperty("violations", List.copyOf(violations));
        return problem;
    }

    public record Violation(String field, String code) {}
}
