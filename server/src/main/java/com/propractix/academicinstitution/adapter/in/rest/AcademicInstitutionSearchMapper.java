package com.propractix.academicinstitution.adapter.in.rest;

import com.propractix.academicinstitution.application.query.AcademicInstitutionSearchPage;
import com.propractix.academicinstitution.application.query.NormalizedInstitutionSearchTerm;
import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;
import java.util.Optional;
import org.springframework.util.MultiValueMap;

public final class AcademicInstitutionSearchMapper {
    public static final int MAX_QUERY_CODE_POINTS = 300;
    private final int defaultLimit;
    private final int maxLimit;
    private final InstitutionSearchCursorCodec cursors;

    public AcademicInstitutionSearchMapper(int defaultLimit, int maxLimit, InstitutionSearchCursorCodec cursors) {
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
        this.cursors = cursors;
    }

    public SearchPublishedInstitutionsQuery toQuery(MultiValueMap<String, String> parameters) {
        String rawQuery = single(parameters, "query");
        String rawCursor = single(parameters, "cursor");
        String rawLimit = single(parameters, "limit");
        Optional<NormalizedInstitutionSearchTerm> term = term(rawQuery);
        int limit = limit(rawLimit);
        var cursor = Optional.ofNullable(rawCursor).map(cursors::decode);
        try {
            return new SearchPublishedInstitutionsQuery(term, cursor, limit);
        } catch (IllegalArgumentException failure) {
            throw new InvalidSearchParameter("cursor");
        }
    }

    public AcademicInstitutionSearchResponse toResponse(AcademicInstitutionSearchPage page) {
        return new AcademicInstitutionSearchResponse(page.items().stream()
                .map(item -> new AcademicInstitutionSearchResponse.Item(
                        item.id().value(), item.officialName().value(), item.countryCode().value()))
                .toList(), page.nextCursor().map(cursors::encode).orElse(null),
                new AcademicInstitutionSearchResponse.Snapshot(
                        page.retrievedAt(), page.sourceUrl(), page.sourceAttribution()));
    }

    private static String single(MultiValueMap<String, String> parameters, String name) {
        var values = parameters.get(name);
        if (values == null) return null;
        if (values.size() != 1 || values.getFirst() == null) throw new InvalidSearchParameter(name);
        return values.getFirst();
    }

    private static Optional<NormalizedInstitutionSearchTerm> term(String raw) {
        if (raw == null || raw.isEmpty()) return Optional.empty();
        if (raw.codePointCount(0, raw.length()) > MAX_QUERY_CODE_POINTS
                || raw.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                        || Character.getType(codePoint) == Character.SURROGATE
                        || Character.getType(codePoint) == Character.LINE_SEPARATOR
                        || Character.getType(codePoint) == Character.PARAGRAPH_SEPARATOR)) {
            throw new InvalidSearchParameter("query");
        }
        if (raw.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            return Optional.empty();
        }
        try {
            return Optional.of(NormalizedInstitutionSearchTerm.from(raw));
        } catch (IllegalArgumentException failure) {
            throw new InvalidSearchParameter("query");
        }
    }

    private int limit(String raw) {
        if (raw == null) return defaultLimit;
        try {
            if (!raw.matches("[0-9]+")) throw new IllegalArgumentException();
            int parsed = Integer.parseInt(raw);
            if (parsed < SearchPublishedInstitutionsQuery.MIN_LIMIT || parsed > maxLimit) {
                throw new IllegalArgumentException();
            }
            return parsed;
        } catch (IllegalArgumentException failure) {
            throw new InvalidSearchParameter("limit");
        }
    }
}
