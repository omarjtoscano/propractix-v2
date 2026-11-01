package com.propractix.academicinstitution.adapter.out.observability;

import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutions;
import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutionsResult;
import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;

/** The complete label space is fixed here; no request value is retained. */
public final class MeteredInstitutionSearch implements SearchPublishedInstitutions {
    private final SearchPublishedInstitutions delegate;
    private final MeterRegistry registry;
    private final Map<String, Timer> timers;

    public MeteredInstitutionSearch(SearchPublishedInstitutions delegate, MeterRegistry registry) {
        this.delegate = delegate;
        this.registry = registry;
        timers = Map.of(
                "found", timer(registry, "found"),
                "snapshot_changed", timer(registry, "snapshot_changed"),
                "unavailable", timer(registry, "unavailable"),
                "failed", timer(registry, "failed"));
    }

    @Override
    public SearchPublishedInstitutionsResult search(SearchPublishedInstitutionsQuery query) {
        Timer.Sample sample = Timer.start(registry);
        String outcome = "failed";
        try {
            var result = delegate.search(query);
            outcome = outcome(result);
            return result;
        } finally {
            sample.stop(timers.get(outcome));
        }
    }

    private static String outcome(SearchPublishedInstitutionsResult result) {
        if (result instanceof SearchPublishedInstitutionsResult.Found) return "found";
        if (result instanceof SearchPublishedInstitutionsResult.SnapshotChanged) return "snapshot_changed";
        return "unavailable";
    }

    private static Timer timer(MeterRegistry registry, String outcome) {
        return Timer.builder("propractix_catalog_search_duration")
                .description("Catalog use-case duration, including persistence; excludes S0 admission")
                .tag("outcome", outcome).register(registry);
    }
}
