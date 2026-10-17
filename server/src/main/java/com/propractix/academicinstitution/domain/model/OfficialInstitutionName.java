package com.propractix.academicinstitution.domain.model;

import java.text.Normalizer;
import java.util.Objects;

public record OfficialInstitutionName(String value) {

    public static final int MAX_CODE_POINTS = 300;

    public OfficialInstitutionName {
        Objects.requireNonNull(value, "value must not be null");
        value = Normalizer.normalize(value, Normalizer.Form.NFC);

        if (!value.isEmpty()
                && (isWhitespace(value.codePointAt(0))
                        || isWhitespace(value.codePointBefore(value.length())))) {
            throw new IllegalArgumentException("value must not contain leading or trailing whitespace");
        }

        int codePointCount = value.codePointCount(0, value.length());
        if (codePointCount < 1 || codePointCount > MAX_CODE_POINTS) {
            throw new IllegalArgumentException("value must contain between 1 and 300 code points");
        }

        if (value.codePoints().anyMatch(OfficialInstitutionName::isForbiddenCodePoint)) {
            throw new IllegalArgumentException("value must not contain controls or line breaks");
        }
    }

    private static boolean isForbiddenCodePoint(int codePoint) {
        int type = Character.getType(codePoint);
        return Character.isISOControl(codePoint)
                || type == Character.LINE_SEPARATOR
                || type == Character.PARAGRAPH_SEPARATOR;
    }

    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    @Override
    public String toString() {
        return value;
    }
}
