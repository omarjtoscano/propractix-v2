package com.propractix.organization.application.port.out;

import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Jurisdiction;
import com.propractix.organization.domain.model.CountryCode;
import com.propractix.organization.domain.service.CompanyEmailAdmissionPolicy;
import com.propractix.organization.domain.service.CompanyRegistrationPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Server context resolves jurisdiction explicitly, independently of presentation fallback. */
public interface CompanyOnboardingPolicyPort {
    Optional<Snapshot> current(CountryCode registeredCountry, Instant at);

    record Snapshot(CompanyEmailAdmissionPolicy admission, CompanyRegistrationPolicy registration,
            Jurisdiction privacyJurisdiction, Duration validity, Duration routingRetryDelay) {
        public Snapshot {
            Objects.requireNonNull(admission, "admission must not be null");
            Objects.requireNonNull(registration, "registration must not be null");
            Objects.requireNonNull(privacyJurisdiction, "privacyJurisdiction must not be null");
            requirePositive(validity);
            requirePositive(routingRetryDelay);
        }

        private static void requirePositive(Duration duration) {
            if (duration == null || duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("policy duration must be positive");
            }
        }
    }
}
