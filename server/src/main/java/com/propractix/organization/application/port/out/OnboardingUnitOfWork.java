package com.propractix.organization.application.port.out;

/**
 * Organization-local atomic repository/outbox/audit boundary. ROLLED_BACK guarantees
 * no writes; UNKNOWN must preserve the operation for reconciliation, never blindly retry.
 * Runtime exceptions roll back and propagate; adapters translate only known recoverable failures.
 */
public interface OnboardingUnitOfWork {
    CommitOutcome execute(Runnable writes);

    enum CommitOutcome { COMMITTED, ROLLED_BACK, UNKNOWN }
}
