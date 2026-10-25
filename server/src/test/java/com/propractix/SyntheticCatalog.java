package com.propractix;

import static org.assertj.core.api.Assertions.assertThat;

import com.propractix.academicinstitution.application.command.PublishInstitutionCatalogCommand;
import com.propractix.academicinstitution.application.port.in.CatalogPublicationResult;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/** Publishes synthetic data through the accepted import use case, never via an HTTP route. */
final class SyntheticCatalog {
    static final String SOURCE = "https://example.invalid/synthetic-catalog.csv";
    static final String ATTRIBUTION = "ProPractix synthetic REST test fixture";
    static final String RETRIEVED_AT = "2026-10-23T00:00:00Z";

    private SyntheticCatalog() {}

    static void publish(PublishInstitutionCatalog publisher, String... names) {
        try {
            var csv = new StringBuilder("sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus\n");
            for (int i = 0; i < names.length; i++) {
                csv.append("SYNTHETIC,INTERNAL-REF-").append(i).append(',').append(names[i])
                        .append(",ES,INTERNAL-STATUS\n");
            }
            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            var hash = new CatalogArtifactHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            assertThat(publisher.publish(new PublishInstitutionCatalogCommand(bytes,
                    "academic-institution-catalog/v1", URI.create(SOURCE), Instant.parse(RETRIEVED_AT), ATTRIBUTION,
                    hash, Optional.empty()))).isInstanceOf(CatalogPublicationResult.Published.class);
        } catch (java.security.NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }
}
