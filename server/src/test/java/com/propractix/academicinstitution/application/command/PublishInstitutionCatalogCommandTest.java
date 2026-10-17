package com.propractix.academicinstitution.application.command;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PublishInstitutionCatalogCommandTest {

    @Test
    void copiesArtifactBytesOnConstructionAndEveryRead() {
        byte[] original = "exact,csv,bytes\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] expected = original.clone();
        PublishInstitutionCatalogCommand command = new PublishInstitutionCatalogCommand(
                original,
                "academic-institution-catalog/v1",
                URI.create("https://example.test/catalog.csv"),
                Instant.parse("2026-10-17T08:00:00Z"),
                "Catalog source",
                new CatalogArtifactHash("a".repeat(64)),
                Optional.empty());

        original[0] = 'X';
        byte[] firstRead = command.artifactBytes();
        firstRead[1] = 'Y';
        byte[] secondRead = command.artifactBytes();

        assertArrayEquals(expected, secondRead);
        assertNotSame(firstRead, secondRead);
    }
}
