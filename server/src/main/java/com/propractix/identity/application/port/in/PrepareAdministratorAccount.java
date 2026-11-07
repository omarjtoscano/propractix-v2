package com.propractix.identity.application.port.in;

import java.util.Objects;
import java.util.UUID;

/**
 * Contract only. Invoked after commit by a future organization handler, never by submit.
 * Input is transient and must not be serialized, logged or copied to durable messages.
 * Implementations must replay the original result by onboardingId; E5 owns provisioning.
 */
public interface PrepareAdministratorAccount {
    Result prepare(Command command);

    enum Outcome {
        NEW_ACCOUNT_PREPARED,
        PENDING_ACCOUNT_RECOVERY_REQUIRED,
        ACCOUNT_CONTINUATION_REQUIRED,
        RETRYABLE_FAILURE,
        NOT_PREPARED
    }

    record Command(UUID onboardingId, String canonicalEmail, String administratorName,
            String locale, UUID correlationId) {
        public Command {
            requireV4(onboardingId);
            Objects.requireNonNull(correlationId, "correlationId must not be null");
            requireText(canonicalEmail);
            requireText(administratorName);
            requireText(locale);
        }

        @Override
        public String toString() {
            return "PrepareAdministratorAccount.Command[redacted]";
        }
    }

    /** No account state, credentials or personal data in a result. */
    record Result(UUID onboardingId, Outcome outcome, java.util.Optional<UUID> userId) {
        public Result {
            requireV4(onboardingId);
            Objects.requireNonNull(outcome, "outcome must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
            boolean prepared = outcome == Outcome.NEW_ACCOUNT_PREPARED
                    || outcome == Outcome.PENDING_ACCOUNT_RECOVERY_REQUIRED
                    || outcome == Outcome.ACCOUNT_CONTINUATION_REQUIRED;
            if (prepared != userId.isPresent()) {
                throw new IllegalArgumentException("account result has inconsistent IDs");
            }
            userId.ifPresent(PrepareAdministratorAccount::requireV4);
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("transient account input must not be blank");
        }
    }

    private static void requireV4(UUID value) {
        Objects.requireNonNull(value, "id must not be null");
        if (value.version() != 4 || value.variant() != 2) {
            throw new IllegalArgumentException("id must be UUID v4");
        }
    }
}
