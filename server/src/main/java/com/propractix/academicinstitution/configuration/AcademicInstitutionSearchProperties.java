package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "propractix.academic-institution.search", ignoreUnknownFields = false)
public record AcademicInstitutionSearchProperties(
        @Min(SearchPublishedInstitutionsQuery.MIN_LIMIT) int defaultLimit,
        @Min(SearchPublishedInstitutionsQuery.MIN_LIMIT) @Max(SearchPublishedInstitutionsQuery.MAX_LIMIT) int maxLimit) {

    @AssertTrue(message = "default limit must not exceed the maximum limit")
    public boolean isDefaultWithinMaximum() {
        return defaultLimit <= maxLimit;
    }
}
