package com.propractix.academicinstitution.adapter.in.rest;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AcademicInstitutionSearchResponse(List<Item> items, String nextCursor, Snapshot snapshot) {
    public record Item(UUID id, String officialName, String countryCode) {}
    public record Snapshot(Instant retrievedAt, URI sourceUrl, String sourceAttribution) {}
}
