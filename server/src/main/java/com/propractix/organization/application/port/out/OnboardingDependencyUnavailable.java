package com.propractix.organization.application.port.out;

/** Known recoverable read/protection failure; contains no external exception message. */
public final class OnboardingDependencyUnavailable extends RuntimeException {
    public OnboardingDependencyUnavailable() {
        super("onboarding dependency unavailable");
    }
}
