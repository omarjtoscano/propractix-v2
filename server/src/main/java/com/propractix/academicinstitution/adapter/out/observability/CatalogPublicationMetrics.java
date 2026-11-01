package com.propractix.academicinstitution.adapter.out.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Administrative attempts include validation failures and unchanged active artifacts. */
public final class CatalogPublicationMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPublicationMetrics.class);
    private final MeterRegistry registry;

    public CatalogPublicationMetrics(MeterRegistry registry) {
        this.registry = registry;
        registry.counter("propractix_catalog_publication_attempts_total", "outcome", "success");
        registry.counter("propractix_catalog_publication_attempts_total", "outcome", "failure");
    }

    public void completed(int exitCode) {
        String outcome = exitCode == 0 ? "success" : "failure";
        registry.counter("propractix_catalog_publication_attempts_total", "outcome", outcome).increment();
        // A CLI registry ends with the process. This bounded event on stderr is the durable signal.
        LOGGER.info("{}", outcome);
    }

    public static void startupFailed() {
        LOGGER.info("failure");
    }
}
