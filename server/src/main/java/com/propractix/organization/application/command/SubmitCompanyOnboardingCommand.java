package com.propractix.organization.application.command;

import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Reference;
import com.propractix.organization.domain.model.CompanyProfile;
import com.propractix.organization.domain.model.EmailAddress;
import java.util.Objects;
import java.util.UUID;

/** Canonical, syntactically validated input; normalization adapters are outside E2. */
public record SubmitCompanyOnboardingCommand(CompanyProfile profile, String administratorName,
        EmailAddress administratorEmail, Reference privacyNoticeReference, UUID correlationId) {
    public SubmitCompanyOnboardingCommand {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(administratorName, "administratorName must not be null");
        Objects.requireNonNull(administratorEmail, "administratorEmail must not be null");
        Objects.requireNonNull(privacyNoticeReference, "privacyNoticeReference must not be null");
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        if (administratorName.isBlank() || administratorName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("administratorName must be non-blank without controls");
        }
    }

    @Override
    public String toString() {
        return "SubmitCompanyOnboardingCommand[redacted]";
    }
}
