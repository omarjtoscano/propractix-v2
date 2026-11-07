package com.propractix.organization.application.port.in;

import com.propractix.organization.application.command.SubmitCompanyOnboardingCommand;
import java.util.Objects;
import java.util.UUID;

public interface SubmitCompanyOnboarding {
    Result submit(SubmitCompanyOnboardingCommand command);

    sealed interface Result permits Received, Rejected, TemporarilyUnavailable, CommitUncertain { }

    /** Confirmed durable receipt. The opaque operation ID is neither a tenant nor an account ID. */
    record Received(UUID operationId) implements Result {
        public Received {
            Objects.requireNonNull(operationId, "operationId must not be null");
        }
    }

    enum Failure {
        PRIVACY_POLICY_UNAVAILABLE,
        PRIVACY_NOTICE_CHANGED,
        COMPANY_EMAIL_POLICY_UNAVAILABLE,
        COMPANY_EMAIL_DOMAIN_NOT_ALLOWED,
        VALIDATION_ERROR
    }

    record Rejected(Failure failure) implements Result {
        public Rejected {
            Objects.requireNonNull(failure, "failure must not be null");
        }
    }

    /** Only returned when the boundary guarantees that no write committed. */
    record TemporarilyUnavailable() implements Result { }

    /**
     * Neither receipt nor rollback is confirmed. This ID identifies the attempted operation,
     * not a durable receipt. Reconciliation is required before any retry; no blind resubmission.
     */
    record CommitUncertain(UUID operationId) implements Result {
        public CommitUncertain {
            Objects.requireNonNull(operationId, "operationId must not be null");
        }
    }
}
