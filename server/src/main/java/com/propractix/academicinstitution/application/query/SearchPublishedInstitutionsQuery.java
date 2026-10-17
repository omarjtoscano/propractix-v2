package com.propractix.academicinstitution.application.query;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

public record SearchPublishedInstitutionsQuery(
        Optional<NormalizedInstitutionSearchTerm> term,
        Optional<InstitutionSearchCursor> cursor,
        int limit) {

    public static final int MIN_LIMIT = 1;
    public static final int MAX_LIMIT = 50;

    public SearchPublishedInstitutionsQuery {
        Objects.requireNonNull(term, "term must not be null");
        Objects.requireNonNull(cursor, "cursor must not be null");
        if (limit < MIN_LIMIT || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and 50");
        }
        String expectedFingerprint = fingerprintFor(term);
        if (cursor.filter(value -> !value.queryFingerprint().equals(expectedFingerprint)).isPresent()) {
            throw new IllegalArgumentException("cursor must belong to the normalized query");
        }
    }

    public String queryFingerprint() {
        return fingerprintFor(term);
    }

    private static String fingerprintFor(Optional<NormalizedInstitutionSearchTerm> term) {
        String value = term.map(NormalizedInstitutionSearchTerm::value).orElse("");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
