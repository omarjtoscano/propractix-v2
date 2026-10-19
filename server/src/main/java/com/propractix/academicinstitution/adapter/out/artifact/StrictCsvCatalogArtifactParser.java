package com.propractix.academicinstitution.adapter.out.artifact;

import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import com.propractix.academicinstitution.application.port.out.CatalogInstitutionCandidate;
import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.ExternalInstitutionReference;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class StrictCsvCatalogArtifactParser implements CatalogArtifactParser {

    static final String EXACT_HEADER = "sourceSystem,sourceRecordId,officialName,countryCode,sourceStatus";

    private final Set<String> allowedSourceSystems;
    private final Set<String> allowedCountries;

    public StrictCsvCatalogArtifactParser(Set<String> allowedSourceSystems, Set<String> allowedCountries) {
        this.allowedSourceSystems = Set.copyOf(
                Objects.requireNonNull(allowedSourceSystems, "allowedSourceSystems must not be null"));
        this.allowedCountries =
                Set.copyOf(Objects.requireNonNull(allowedCountries, "allowedCountries must not be null"));
        if (this.allowedSourceSystems.isEmpty() || this.allowedCountries.isEmpty()) {
            throw new IllegalArgumentException("allowed sources and countries must not be empty");
        }
    }

    @Override
    public ParseResult parse(byte[] exactArtifactBytes) {
        Objects.requireNonNull(exactArtifactBytes, "exactArtifactBytes must not be null");
        if (hasUtf8Bom(exactArtifactBytes)) {
            return rejected(Failure.INVALID_ENCODING);
        }

        String csv;
        try {
            csv = decodeStrictUtf8(exactArtifactBytes);
        } catch (CharacterCodingException exception) {
            return rejected(Failure.INVALID_ENCODING);
        }

        if (!hasExactlyOneFinalLf(csv) || csv.indexOf('\r') >= 0) {
            return rejected(Failure.INVALID_SCHEMA);
        }
        int headerEnd = csv.indexOf('\n');
        if (headerEnd < 0 || !EXACT_HEADER.equals(csv.substring(0, headerEnd))) {
            return rejected(Failure.INVALID_SCHEMA);
        }
        if (headerEnd == csv.length() - 1) {
            return rejected(Failure.INVALID_SCHEMA);
        }

        String data = csv.substring(headerEnd + 1, csv.length() - 1);
        List<List<String>> records;
        try {
            records = parseRecords(data);
        } catch (InvalidCsvException exception) {
            return rejected(Failure.INVALID_SCHEMA);
        }

        List<CatalogInstitutionCandidate> candidates = new ArrayList<>(records.size());
        Set<ExternalInstitutionReference> references = new HashSet<>();
        try {
            for (List<String> record : records) {
                if (record.size() != 5 || isEmptyRecord(record)) {
                    return rejected(Failure.INVALID_SCHEMA);
                }
                CatalogInstitutionCandidate candidate = toCandidate(record);
                if (!references.add(candidate.externalReference())) {
                    return rejected(Failure.INVALID_CONTENT);
                }
                candidates.add(candidate);
            }
        } catch (IllegalArgumentException exception) {
            return rejected(Failure.INVALID_CONTENT);
        }
        return new ParsedArtifact(candidates, records.size(), 0);
    }

    private CatalogInstitutionCandidate toCandidate(List<String> fields) {
        String sourceSystem = requireUnmodifiedToken(fields.get(0), "sourceSystem");
        String sourceRecordId = requireUnmodifiedToken(fields.get(1), "sourceRecordId");
        String officialNameValue = fields.get(2);
        String countryCode = requireUnmodifiedToken(fields.get(3), "countryCode");
        String sourceStatusValue = fields.get(4);

        if (!allowedSourceSystems.contains(sourceSystem) || !allowedCountries.contains(countryCode)) {
            throw new IllegalArgumentException("source or country is not allowed");
        }
        if (!Normalizer.isNormalized(officialNameValue, Normalizer.Form.NFC)) {
            throw new IllegalArgumentException("officialName must already be NFC");
        }
        OfficialInstitutionName officialName = new OfficialInstitutionName(officialNameValue);
        if (!officialName.value().equals(officialNameValue)) {
            throw new IllegalArgumentException("officialName must not be normalized by the parser");
        }

        Optional<String> sourceStatus;
        if (sourceStatusValue.isEmpty()) {
            sourceStatus = Optional.empty();
        } else {
            sourceStatus = Optional.of(requireUnmodifiedToken(sourceStatusValue, "sourceStatus"));
        }
        return new CatalogInstitutionCandidate(
                new ExternalInstitutionReference(sourceSystem, sourceRecordId),
                officialName,
                new CountryCode(countryCode),
                sourceStatus);
    }

    private static String requireUnmodifiedToken(String value, String fieldName) {
        if (value.isEmpty() || hasExteriorWhitespace(value) || value.codePoints().anyMatch(StrictCsvCatalogArtifactParser::isForbiddenCodePoint)) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return value;
    }

    private static boolean hasExteriorWhitespace(String value) {
        return isWhitespace(value.codePointAt(0)) || isWhitespace(value.codePointBefore(value.length()));
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

    private static boolean isEmptyRecord(List<String> record) {
        return record.size() == 1 && record.getFirst().isEmpty();
    }

    private static List<List<String>> parseRecords(String data) throws InvalidCsvException {
        if (data.isEmpty()) {
            return List.of();
        }
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>(5);
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;
        boolean fieldStarted = false;

        for (int index = 0; index < data.length(); index++) {
            char current = data.charAt(index);
            if (quoted) {
                if (current == '"') {
                    if (index + 1 < data.length() && data.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        quoted = false;
                        quoteClosed = true;
                    }
                } else {
                    field.append(current);
                }
                continue;
            }

            if (quoteClosed) {
                if (current == ',') {
                    record.add(field.toString());
                    field.setLength(0);
                    quoteClosed = false;
                    fieldStarted = false;
                } else if (current == '\n') {
                    record.add(field.toString());
                    records.add(List.copyOf(record));
                    record.clear();
                    field.setLength(0);
                    quoteClosed = false;
                    fieldStarted = false;
                } else {
                    throw new InvalidCsvException();
                }
                continue;
            }

            if (current == '"') {
                if (fieldStarted || !field.isEmpty()) {
                    throw new InvalidCsvException();
                }
                quoted = true;
                fieldStarted = true;
            } else if (current == ',') {
                record.add(field.toString());
                field.setLength(0);
                fieldStarted = false;
            } else if (current == '\n') {
                record.add(field.toString());
                records.add(List.copyOf(record));
                record.clear();
                field.setLength(0);
                fieldStarted = false;
            } else {
                field.append(current);
                fieldStarted = true;
            }
        }
        if (quoted) {
            throw new InvalidCsvException();
        }
        record.add(field.toString());
        records.add(List.copyOf(record));
        return List.copyOf(records);
    }

    private static String decodeStrictUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb
                && (bytes[2] & 0xff) == 0xbf;
    }

    private static boolean hasExactlyOneFinalLf(String value) {
        return value.endsWith("\n") && (value.length() == 1 || value.charAt(value.length() - 2) != '\n');
    }

    private static RejectedArtifact rejected(Failure failure) {
        return new RejectedArtifact(failure);
    }

    private static final class InvalidCsvException extends Exception {}
}
