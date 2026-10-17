package com.propractix.academicinstitution.domain.model;

import java.util.Objects;

public record ExternalInstitutionReference(String sourceSystem, String sourceRecordId) {

    public ExternalInstitutionReference {
        sourceSystem = normalizeRequired(sourceSystem, "sourceSystem");
        sourceRecordId = normalizeRequired(sourceRecordId, "sourceRecordId");
    }

    private static String normalizeRequired(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = stripExteriorWhitespace(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String stripExteriorWhitespace(String value) {
        int start = 0;
        int end = value.length();
        while (start < end) {
            int codePoint = value.codePointAt(start);
            if (!isWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        while (end > start) {
            int codePoint = value.codePointBefore(end);
            if (!isWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return value.substring(start, end);
    }

    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
