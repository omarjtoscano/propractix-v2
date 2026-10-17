package com.propractix.academicinstitution.application.query;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public record NormalizedInstitutionSearchTerm(String value) {

    public NormalizedInstitutionSearchTerm {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }
        String normalized = normalize(value);
        if (!value.equals(normalized)) {
            throw new IllegalArgumentException("value must already be normalized");
        }
    }

    public static NormalizedInstitutionSearchTerm from(String rawValue) {
        Objects.requireNonNull(rawValue, "rawValue must not be null");
        String normalized = normalize(rawValue);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("rawValue must contain searchable characters");
        }
        return new NormalizedInstitutionSearchTerm(normalized);
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        String withoutDiacritics = decomposed.codePoints()
                .filter(codePoint -> {
                    int type = Character.getType(codePoint);
                    return type != Character.NON_SPACING_MARK
                            && type != Character.COMBINING_SPACING_MARK
                            && type != Character.ENCLOSING_MARK;
                })
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .toLowerCase(Locale.ROOT);
        StringBuilder collapsed = new StringBuilder();
        boolean pendingSpace = false;
        for (int offset = 0; offset < withoutDiacritics.length(); ) {
            int codePoint = withoutDiacritics.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                pendingSpace = collapsed.length() > 0;
            } else {
                if (pendingSpace) {
                    collapsed.append(' ');
                    pendingSpace = false;
                }
                collapsed.appendCodePoint(codePoint);
            }
        }
        return Normalizer.normalize(collapsed, Normalizer.Form.NFC);
    }
}
