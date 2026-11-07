package com.propractix.organization.application.port.out;

import java.time.Instant;

public interface OnboardingClock {
    Instant now();
}
