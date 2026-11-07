package com.propractix.organization.application.port.out;

import com.propractix.organization.application.contract.event.v1.CompanyOnboardingEventV1;

/** Enlists in the same local unit of work as the onboarding; never delivers inline. */
public interface OnboardingOutbox {
    void append(CompanyOnboardingEventV1 event);
}
