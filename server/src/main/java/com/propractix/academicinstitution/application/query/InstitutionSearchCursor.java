package com.propractix.academicinstitution.application.query;

import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record InstitutionSearchCursor(
        int version,
        CatalogImportId snapshotId,
        String queryFingerprint,
        String lastSearchNameNormalized,
        AcademicInstitutionId lastInstitutionId) {

    public static final int CURRENT_VERSION = 1;
    private static final Pattern SHA_256_HEXADECIMAL = Pattern.compile("[0-9a-fA-F]{64}");

    public InstitutionSearchCursor {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("unsupported cursor version");
        }
        Objects.requireNonNull(snapshotId, "snapshotId must not be null");
        Objects.requireNonNull(queryFingerprint, "queryFingerprint must not be null");
        if (!SHA_256_HEXADECIMAL.matcher(queryFingerprint).matches()) {
            throw new IllegalArgumentException("queryFingerprint must be a SHA-256 hexadecimal hash");
        }
        queryFingerprint = queryFingerprint.toLowerCase(Locale.ROOT);
        Objects.requireNonNull(lastSearchNameNormalized, "lastSearchNameNormalized must not be null");
        if (lastSearchNameNormalized.isBlank()) {
            throw new IllegalArgumentException("lastSearchNameNormalized must not be blank");
        }
        Objects.requireNonNull(lastInstitutionId, "lastInstitutionId must not be null");
    }

    public static InstitutionSearchCursor after(
            CatalogImportId snapshotId,
            String queryFingerprint,
            String lastSearchNameNormalized,
            AcademicInstitutionId lastInstitutionId) {
        return new InstitutionSearchCursor(
                CURRENT_VERSION,
                snapshotId,
                queryFingerprint,
                lastSearchNameNormalized,
                lastInstitutionId);
    }
}
