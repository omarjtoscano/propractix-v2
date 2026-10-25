package com.propractix.academicinstitution.adapter.in.rest;

import com.propractix.academicinstitution.application.query.InstitutionSearchCursor;
import com.propractix.academicinstitution.application.query.NormalizedInstitutionSearchTerm;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import java.util.Base64;
import java.util.Set;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Transport encoding only. The application checks query and snapshot binding. */
public final class InstitutionSearchCursorCodec {
    public static final int MAX_ENCODED_LENGTH = 4096;
    private static final Set<String> FIELDS = Set.of(
            "version", "snapshotId", "queryFingerprint", "lastSearchNameNormalized", "lastInstitutionId");
    private final JsonMapper json;

    public InstitutionSearchCursorCodec(JsonMapper json) {
        this.json = json.rebuild()
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
    }

    public String encode(InstitutionSearchCursor cursor) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(new Payload(
                cursor.version(), cursor.snapshotId().toString(), cursor.queryFingerprint(),
                cursor.lastSearchNameNormalized(), cursor.lastInstitutionId().toString())));
    }

    public InstitutionSearchCursor decode(String encoded) {
        try {
            if (encoded.isEmpty() || encoded.length() > MAX_ENCODED_LENGTH
                    || !encoded.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException();
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(encoded)) {
                throw new IllegalArgumentException();
            }
            var payload = json.readTree(bytes);
            if (!payload.isObject() || !payload.propertyNames().equals(FIELDS)
                    || !payload.path("version").isIntegralNumber()
                    || !payload.path("version").canConvertToInt()) throw new IllegalArgumentException();
            for (String field : FIELDS) {
                if (!field.equals("version") && !payload.path(field).isString()) throw new IllegalArgumentException();
            }
            String name = payload.path("lastSearchNameNormalized").asText();
            // Bound transport size without applying display-name rules to the normalized order key.
            if (name.isEmpty() || name.length() > MAX_ENCODED_LENGTH
                    || name.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                            || Character.getType(codePoint) == Character.SURROGATE)) throw new IllegalArgumentException();
            new NormalizedInstitutionSearchTerm(name);
            return new InstitutionSearchCursor(payload.path("version").asInt(),
                    CatalogImportId.from(payload.path("snapshotId").asText()),
                    payload.path("queryFingerprint").asText(), name,
                    AcademicInstitutionId.from(payload.path("lastInstitutionId").asText()));
        } catch (RuntimeException failure) {
            // Do not retain the input, Jackson exception, or decoded cursor in the public failure.
            throw new InvalidSearchParameter("cursor");
        }
    }

    private record Payload(int version, String snapshotId, String queryFingerprint,
                           String lastSearchNameNormalized, String lastInstitutionId) {}
}
