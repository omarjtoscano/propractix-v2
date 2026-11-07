package com.propractix.organization.application.port.out;

/**
 * Organization-local atomic repository/outbox/audit boundary. COMMITTED confirms durable
 * receipt of all writes; ROLLED_BACK confirms no writes. UNKNOWN confirms neither and may
 * retain no writes at all: reconciliation is required before any retry.
 * Adapters report UNKNOWN for ambiguous commit/rollback failures. Exceptions propagate only
 * with confirmed rollback; only known recoverable failures translate to ROLLED_BACK.
 */
public interface OnboardingUnitOfWork {
    CommitOutcome execute(Runnable writes);

    enum CommitOutcome { COMMITTED, ROLLED_BACK, UNKNOWN }
}
