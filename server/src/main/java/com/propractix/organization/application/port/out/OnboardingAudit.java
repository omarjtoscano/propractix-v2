package com.propractix.organization.application.port.out;

import com.propractix.organization.domain.model.OnboardingId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public interface OnboardingAudit {
    void append(Entry entry);

    /** Pre-authentication system action: no actor account or tenant exists yet. */
    record Entry(OnboardingId resourceId, Instant at, UUID correlationId) {
        public Entry {
            Objects.requireNonNull(resourceId, "resourceId must not be null");
            Objects.requireNonNull(at, "at must not be null");
            Objects.requireNonNull(correlationId, "correlationId must not be null");
        }
        public String actor() { return "ANONYMOUS_SUBMISSION"; }
        public String action() { return "COMPANY_ONBOARDING_SUBMITTED"; }
        public String result() { return "RECEIVED"; }
    }
}
