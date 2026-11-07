package com.propractix.organization.application.port.out;

import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Notice;
import com.propractix.organization.domain.model.CompanyOnboarding;
import com.propractix.organization.domain.model.OnboardingId;
import com.propractix.organization.domain.model.TenantId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Pre-tenant records are accessible only to the owning process; never a public lookup/poll. */
public interface CompanyOnboardingRepository {
    void insert(PendingSubmission submission);
    Optional<PendingSubmission> findForProcess(OnboardingId onboardingId);
    Optional<PendingSubmission> findForTenant(TenantId tenantId, OnboardingId onboardingId);

    enum Continuation { AWAIT_IDENTITY, COMPANY_REVIEW_REQUIRED }

    /** Reference and applicability metadata come from compliance, never the client. */
    record PrivacyEvidence(Notice resolvedNotice, Instant recordedAt) {
        public PrivacyEvidence {
            Objects.requireNonNull(resolvedNotice, "resolvedNotice must not be null");
            Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        }
    }

    /**
     * Transient adapter input. An E3 adapter must protect personal fields at rest and use
     * protectedTaxIdentity rather than persisting profile.taxIdentifier.normalizedValue.
     */
    record PendingSubmission(UUID operationId, CompanyOnboarding onboarding, String administratorName,
            TaxIdentityProtectionPort.ProtectedTaxIdentity protectedTaxIdentity,
            PrivacyEvidence privacyEvidence, Continuation continuation) {
        public PendingSubmission {
            Objects.requireNonNull(operationId, "operationId must not be null");
            Objects.requireNonNull(onboarding, "onboarding must not be null");
            Objects.requireNonNull(administratorName, "administratorName must not be null");
            Objects.requireNonNull(protectedTaxIdentity, "protectedTaxIdentity must not be null");
            Objects.requireNonNull(privacyEvidence, "privacyEvidence must not be null");
            Objects.requireNonNull(continuation, "continuation must not be null");
        }

        public boolean belongsTo(TenantId tenantId) {
            return onboarding.tenantId().filter(Objects.requireNonNull(tenantId)::equals).isPresent();
        }

        @Override
        public String toString() { return "PendingSubmission[redacted]"; }
    }
}
