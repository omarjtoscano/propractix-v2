package com.propractix.academicinstitution.adapter.in.rest;

import com.propractix.platform.observability.CorrelationIdFilter;
import com.propractix.platform.web.PublicProblemDetails;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AcademicInstitutionSearchController.class)
public final class AcademicInstitutionSearchExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcademicInstitutionSearchExceptionHandler.class);

    @ExceptionHandler(InvalidSearchParameter.class)
    ProblemDetail invalid(InvalidSearchParameter failure, HttpServletResponse response) {
        return PublicProblemDetails.create(400, "validation_error", AcademicInstitutionSearchController.PATH,
                response.getHeader(CorrelationIdFilter.HEADER_NAME),
                List.of(new PublicProblemDetails.Violation(failure.field(), "validation_error")));
    }

    @ExceptionHandler(RuntimeException.class)
    ProblemDetail unavailable(HttpServletResponse response) {
        LOGGER.error("Academic institution catalog HTTP query failed with category=internal_failure");
        return PublicProblemDetails.create(503, "academic_institution_catalog_unavailable",
                AcademicInstitutionSearchController.PATH, response.getHeader(CorrelationIdFilter.HEADER_NAME), List.of());
    }
}
