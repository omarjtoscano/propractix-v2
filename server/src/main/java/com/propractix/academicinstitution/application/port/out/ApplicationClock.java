package com.propractix.academicinstitution.application.port.out;

import java.time.Instant;

@FunctionalInterface
public interface ApplicationClock {

    Instant now();
}
