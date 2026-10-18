package com.propractix.academicinstitution.adapter.out.artifact;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import com.propractix.academicinstitution.application.port.out.CatalogInstitutionCandidate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StrictCsvCatalogArtifactParserTest {

    private static final String HEADER = StrictCsvCatalogArtifactParser.EXACT_HEADER;
    private static final String FIXTURE_HASH = "612b46e73d3fa6843e79b0efd2b0f253326803b976de5627a58daf6d416cc89a";

    private final StrictCsvCatalogArtifactParser parser =
            new StrictCsvCatalogArtifactParser(Set.of("SYNTHETIC"), Set.of("ES"));

    @Test
    void parsesTheExactSyntheticFixtureWithoutChangingItsBytes() throws IOException {
        Path fixture = Path.of("..", "docs", "catalog", "fixtures", "academic-institutions-es-synthetic-v1.csv");
        byte[] before = Files.readAllBytes(fixture);

        CatalogArtifactParser.ParsedArtifact parsed = assertInstanceOf(
                CatalogArtifactParser.ParsedArtifact.class, parser.parse(before));

        assertEquals(FIXTURE_HASH, sha256(before));
        assertEquals(3, parsed.totalRowCount());
        assertEquals(3, parsed.candidates().size());
        assertEquals(0, parsed.omittedRowCount());
        assertEquals("TEST-0001", parsed.candidates().getFirst().externalReference().sourceRecordId());
        assertEquals("Universidad Sintética de Ejemplo Norte", parsed.candidates().getFirst().officialName().value());
        assertTrue(parsed.candidates().stream().allMatch(candidate -> candidate.sourceStatus().isEmpty()));
        assertArrayEquals(before, Files.readAllBytes(fixture));
    }

    @Test
    void supportsQuotedCommasEscapedQuotesAccentsAndLeadingZeroReferences() {
        String csv = HEADER + "\n"
                + "SYNTHETIC,000042,\"Universidad de \"\"Arte\"\", Música y Diseño\",ES,ACTIVE\n";

        CatalogArtifactParser.ParsedArtifact parsed = assertInstanceOf(
                CatalogArtifactParser.ParsedArtifact.class, parser.parse(bytes(csv)));
        CatalogInstitutionCandidate candidate = parsed.candidates().getFirst();

        assertEquals("000042", candidate.externalReference().sourceRecordId());
        assertEquals("Universidad de \"Arte\", Música y Diseño", candidate.officialName().value());
        assertEquals("ACTIVE", candidate.sourceStatus().orElseThrow());
    }

    @Test
    void acceptsStrictUtf8AndEmptySourceStatusWithoutInferringIt() {
        CatalogArtifactParser.ParsedArtifact parsed = assertInstanceOf(
                CatalogArtifactParser.ParsedArtifact.class,
                parser.parse(bytes(validRow("0001", "Universidad de Málaga", ""))));

        assertEquals("Universidad de Málaga", parsed.candidates().getFirst().officialName().value());
        assertTrue(parsed.candidates().getFirst().sourceStatus().isEmpty());
    }

    @Test
    void rejectsMalformedUtf8AndBom() {
        byte[] malformed = bytes(validRow("0001", "Universidad Alfa", ""));
        malformed[malformed.length - 3] = (byte) 0xc3;
        assertRejected(malformed, CatalogArtifactParser.Failure.INVALID_ENCODING);

        byte[] valid = bytes(validRow("0001", "Universidad Alfa", ""));
        byte[] bom = new byte[valid.length + 3];
        bom[0] = (byte) 0xef;
        bom[1] = (byte) 0xbb;
        bom[2] = (byte) 0xbf;
        System.arraycopy(valid, 0, bom, 3, valid.length);
        assertRejected(bom, CatalogArtifactParser.Failure.INVALID_ENCODING);
    }

    @Test
    void rejectsCrLfAndStandaloneCr() {
        assertRejected(
                bytes(validRow("0001", "Universidad Alfa", "").replace("\n", "\r\n")),
                CatalogArtifactParser.Failure.INVALID_SCHEMA);
        assertRejected(
                bytes(validRow("0001", "Universidad Alfa", "").replace("\n", "\r")),
                CatalogArtifactParser.Failure.INVALID_SCHEMA);
    }

    @Test
    void requiresExactlyOneFinalLf() {
        String valid = validRow("0001", "Universidad Alfa", "");
        assertRejected(bytes(valid.substring(0, valid.length() - 1)), CatalogArtifactParser.Failure.INVALID_SCHEMA);
        assertRejected(bytes(valid + "\n"), CatalogArtifactParser.Failure.INVALID_SCHEMA);
    }

    @Test
    void rejectsIncorrectOrReorderedHeader() {
        assertRejected(
                bytes("sourceRecordId,sourceSystem,officialName,countryCode,sourceStatus\n"
                        + "0001,SYNTHETIC,Universidad Alfa,ES,\n"),
                CatalogArtifactParser.Failure.INVALID_SCHEMA);
        assertRejected(
                bytes(HEADER.replace("sourceStatus", "status") + "\n"
                        + "SYNTHETIC,0001,Universidad Alfa,ES,\n"),
                CatalogArtifactParser.Failure.INVALID_SCHEMA);
    }

    @Test
    void rejectsIncorrectColumnCountsMalformedQuotingAndEmptyRows() {
        assertRejected(
                bytes(HEADER + "\nSYNTHETIC,0001,Universidad Alfa,ES\n"),
                CatalogArtifactParser.Failure.INVALID_SCHEMA);
        assertRejected(
                bytes(HEADER + "\nSYNTHETIC,0001,Universidad Alfa,ES,,EXTRA\n"),
                CatalogArtifactParser.Failure.INVALID_SCHEMA);
        assertRejected(
                bytes(HEADER + "\nSYNTHETIC,0001,\"Universidad Alfa,ES,\n"),
                CatalogArtifactParser.Failure.INVALID_SCHEMA);
        assertRejected(
                bytes(HEADER + "\nSYNTHETIC,0001,Universidad Alfa,ES,\n\n"
                        + "SYNTHETIC,0002,Universidad Beta,ES,\n"),
                CatalogArtifactParser.Failure.INVALID_SCHEMA);
    }

    @Test
    void rejectsNonNfcNamesControlsLineBreaksAndExteriorWhitespace() {
        String decomposed = Normalizer.normalize("Universidad de Málaga", Normalizer.Form.NFD);
        assertRejected(
                bytes(validRow("0001", decomposed, "")), CatalogArtifactParser.Failure.INVALID_CONTENT);
        assertRejected(
                bytes(validRow("0001", " Universidad Alfa", "")),
                CatalogArtifactParser.Failure.INVALID_CONTENT);
        assertRejected(
                bytes(validRow("0001", "Universidad Alfa ", "")),
                CatalogArtifactParser.Failure.INVALID_CONTENT);
        assertRejected(
                bytes(validRow("0001", "Universidad\u0001Alfa", "")),
                CatalogArtifactParser.Failure.INVALID_CONTENT);
        assertRejected(
                bytes(HEADER + "\nSYNTHETIC,0001,\"Universidad\nAlfa\",ES,\n"),
                CatalogArtifactParser.Failure.INVALID_CONTENT);
    }

    @Test
    void rejectsDuplicateExternalReferences() {
        String csv = HEADER + "\n"
                + "SYNTHETIC,0001,Universidad Alfa,ES,\n"
                + "SYNTHETIC,0001,Universidad Beta,ES,\n";

        assertRejected(bytes(csv), CatalogArtifactParser.Failure.INVALID_CONTENT);
    }

    @Test
    void validatesSourcesAndCountriesOnlyAgainstInjectedConfiguration() {
        assertRejected(
                bytes(HEADER + "\nRUCT,0001,Universidad Alfa,ES,\n"),
                CatalogArtifactParser.Failure.INVALID_CONTENT);
        assertRejected(
                bytes(HEADER + "\nSYNTHETIC,0001,Universidad Alfa,FR,\n"),
                CatalogArtifactParser.Failure.INVALID_CONTENT);

        StrictCsvCatalogArtifactParser configured =
                new StrictCsvCatalogArtifactParser(Set.of("RUCT"), Set.of("FR"));
        assertInstanceOf(
                CatalogArtifactParser.ParsedArtifact.class,
                configured.parse(bytes(HEADER + "\nRUCT,0001,Université Exemple,FR,\n")));
    }

    private static String validRow(String recordId, String name, String status) {
        return HEADER + "\nSYNTHETIC," + recordId + "," + name + ",ES," + status + "\n";
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private void assertRejected(byte[] artifact, CatalogArtifactParser.Failure expectedFailure) {
        CatalogArtifactParser.RejectedArtifact rejected = assertInstanceOf(
                CatalogArtifactParser.RejectedArtifact.class, parser.parse(artifact));
        assertEquals(expectedFailure, rejected.failure());
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
